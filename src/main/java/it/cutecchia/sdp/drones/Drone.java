package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.*;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import it.cutecchia.sdp.drones.states.DroneState;
import it.cutecchia.sdp.drones.states.RingMasterState;
import it.cutecchia.sdp.drones.states.RingSlaveState;
import it.cutecchia.sdp.drones.states.StartupState;
import it.cutecchia.sdp.drones.store.DroneStore;
import it.cutecchia.sdp.drones.store.InMemoryDroneStore;
import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class Drone implements DroneCommunicationServer {
  private static final long MASTER_HEARTBEAT_PERIOD = 2 * 1000;
  private static final long PRINT_STATS_PERIOD = 10 * 1000;
  private static final long TIME_TO_RECHARGE = 10 * 1000;

  private final AdminServerClient adminServerClient;
  private final DroneIdentifier identifier;
  private final OrderSource orderSource;
  private final RpcDroneCommunicationMiddleware middleware;
  private final DroneStore store = new InMemoryDroneStore();
  private final PollutionTracker pollutionTracker = new PollutionTracker();
  private final ElectionManager electionManager;
  private final DistributedLock chargingAreaLock;

  private enum ChargingStatus {
    NOT_REQUESTED,
    WAITING_TO_RECHARGE,
    SLEEPING,
    FINALIZING
  }

  private ChargingStatus chargingStatus = ChargingStatus.NOT_REQUESTED;

  private volatile DroneState currentState;
  private final Object localDataLock = new Object();
  private volatile DroneData localData;
  private volatile int totalDeliveredOrders = 0;
  private volatile double totalTravelledDistance = 0.0;

  private final Timer printStatsTimer = new Timer();
  private final TimerTask printStatsTask =
      new TimerTask() {
        @Override
        public void run() {
          Log.userMessage(
              "%d#: Total delivered orders: %d\tDistance travelled: %3f\tRemaining battery: %d%% Master? %s",
              identifier.getId(),
              totalDeliveredOrders,
              totalTravelledDistance,
              getLocalData().getBatteryPercentage(),
              currentState.isMaster() ? "yes" : "no");
          currentState.printStats();
        }
      };

  private final Timer masterHeartbeatTimer = new Timer();
  private final TimerTask masterHeartbeat =
      new TimerTask() {
        @Override
        public void run() {
          deliverToMaster(middleware::requestHeartbeat, () -> {});
        }
      };

  public Drone(
      DroneIdentifier identifier, OrderSource orderSource, AdminServerClient adminServerClient) {
    this.identifier = identifier;
    this.adminServerClient = adminServerClient;
    this.currentState = new StartupState(this, adminServerClient);
    this.middleware = new RpcDroneCommunicationMiddleware(identifier, this);
    this.orderSource = orderSource;
    this.electionManager = new ElectionManager(this, store, middleware);
    this.chargingAreaLock = new DistributedLock(identifier, store, middleware);
  }

  public DroneIdentifier getIdentifier() {
    return identifier;
  }

  public DroneData getLocalData() {
    return localData;
  }

  public boolean isDeliveringOrder() {
    return localData.getAssignedOrder() != null;
  }

  public boolean isMaster() {
    return currentState.isMaster();
  }

  public void start() throws IOException {
    middleware.startRpcServer();
    currentState.start();
    pollutionTracker.startTracking();
  }

  public void printStats() {
    printStatsTask.run();
  }

  private volatile boolean shutdownInitiated = false;

  public synchronized boolean shutdown() {
    if (shutdownInitiated) {
      return false;
    }

    Log.userMessage("This drone has initiated the shutdown procedure");

    shutdownInitiated = true;
    currentState.initiateShutdown();

    return true;
  }

  public synchronized void recharge() {
    if (shutdownInitiated) {
      Log.userMessage("This drone cannot recharge because it has initiated the shutdown procedure");
      return;
    }

    if (chargingStatus != ChargingStatus.NOT_REQUESTED) {
      Log.userMessage(
          "Wait for the previous recharge request to finish before requesting another recharge!");
      return;
    }

    chargingStatus = ChargingStatus.WAITING_TO_RECHARGE;
    assert !chargingAreaLock.isOwned();
    ThreadUtils.runInAnotherThread(
        () -> {
          Log.userMessage("Waiting to get permission to recharge from all other drones...");
          chargingAreaLock.take();

          synchronized (this) {
            synchronized (localDataLock) {
              localData = localData.refuseOrders();
            }
          }
          DataRaceTester.sleepForCollisions();
          Log.debug("Took the lock!");

          chargingStatus = ChargingStatus.SLEEPING;

          DataRaceTester.sleepForCollisions();

          Log.userMessage("I will recharge as soon as I complete my order");
          doWhenThereIsNoOrderToDeliver(
              () -> {
                assert !isDeliveringOrder();

                Log.userMessage(
                    "%d: Starting the recharge. Sleeping...", System.currentTimeMillis());
                try {
                  assert chargingAreaLock.isOwned();

                  chargingStatus = ChargingStatus.SLEEPING;
                  try {
                    Thread.sleep(TIME_TO_RECHARGE);
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                } finally {
                  chargingAreaLock.release();
                  chargingStatus = ChargingStatus.FINALIZING;
                  Log.userMessage("%d: I have finished recharging", System.currentTimeMillis());
                  DataRaceTester.sleepForCollisions();

                  // Invece di statusUpdate potrebbe essere un semplice doneRecharging
                  synchronized (this) {
                    synchronized (localDataLock) {
                      localData = new DroneData(new CityPoint(0, 0), 100);
                    }
                  }

                  Log.debug("deliverToMaster for RECHARGE");
                  deliverToMaster(
                      (master) -> {
                        Log.debug("Deliver to master because of status update (lock release)");
                        return middleware.notifyCompletedCharging(master, identifier);
                      },
                      () -> {
                        Log.debug("Notified that I'm done recharging to master");
                        chargingStatus = ChargingStatus.NOT_REQUESTED;
                      });
                }
                return true;
              });
        });
  }

  public void changeStateTo(DroneState newState) {
    currentState.teardown();
    currentState = newState;
    currentState.start();
  }

  public void onAdminServerAcceptance(CityPoint position, Set<DroneIdentifier> allDrones) {
    Log.userMessage("Drone #%d was accepted by the admin server", identifier.getId());

    synchronized (localDataLock) {
      localData = new DroneData(position);
      localDataLock.notify();
    }

    allDrones.forEach(store::addDrone);

    if (allDrones.size() == 1) {
      store.setKnownMaster(identifier);
      changeStateTo(
          new RingMasterState(
              this, store, middleware, adminServerClient, orderSource, electionManager));
    } else {
      changeStateTo(
          new RingSlaveState(this, store, middleware, adminServerClient, electionManager));
    }
    masterHeartbeatTimer.scheduleAtFixedRate(masterHeartbeat, 0, MASTER_HEARTBEAT_PERIOD);
    printStatsTimer.scheduleAtFixedRate(printStatsTask, 0, PRINT_STATS_PERIOD);
  }

  @Override
  public DroneJoinResponse onDroneJoin(DroneIdentifier identifier, CityPoint startingPosition) {
    store.addDrone(identifier);
    DataRaceTester.sleepForCollisions();
    store.handleDroneUpdateData(identifier, new DroneData(startingPosition));
    Log.notice("A new drone (#%d) joined the ring at %s", identifier.getId(), startingPosition);
    currentState.onNewDroneJoin(identifier);

    return new DroneJoinResponse(getIdentifier(), currentState.isMaster());
  }

  private double calculateTotalTravelledDistanceForOrder(CityPoint startingPosition, Order order) {
    return startingPosition.distanceTo(order.getStartPoint())
        + order.getStartPoint().distanceTo(order.getDeliveryPoint());
  }

  private void deliverToMaster(Function<DroneIdentifier, Boolean> send, Runnable onSuccess) {
    final DroneIdentifier knownMaster = store.getKnownMaster();
    if (knownMaster != null) {
      if (send.apply(knownMaster)) {
        onSuccess.run();
        return;
      }

      store.signalFailedCommunicationWithDrone(knownMaster);
    }

    synchronized (electionManager) {
      final ElectionManager.OnNewMasterElectedListener onNewMasterElectedListener =
          (thisListener) -> {
            if (send.apply(store.getKnownMaster())) {
              electionManager.clearOnNewMasterElectedListener(thisListener);
              onSuccess.run();
            } else {
              electionManager.beginElection();
            }
          };

      electionManager.addOnNewMasterElectedListener(onNewMasterElectedListener);
      if (store.getKnownMaster() != null) {
        // Edge case: the master was elected just before we entered the synchronized block
        // Ugly fix: We trigger the callback manually
        Log.warn("I had to manually start the newMasterElected callback");
        ThreadUtils.runInAnotherThread(
            () -> onNewMasterElectedListener.onNewMasterElected(onNewMasterElectedListener));
        return;
      }

      // Check if we're currently in the middle of an election. If we're not then we start a new one
      electionManager.waitUntilNoElectionIsHappening();
      if (store.getKnownMaster() == null) {
        electionManager.beginElection();
      }
    }
  }

  @Override
  public synchronized boolean onOrderAssigned(Order order) {
    if (!localData.canAcceptOrders()) {
      Log.info("Refusing to deliver order %s because I'm recharging", order.getId());
      return false;
    }

    Log.userMessage(
        "%d: I was assigned order %s. Delivering...", System.currentTimeMillis(), order);
    assert !isDeliveringOrder();

    synchronized (localDataLock) {
      localData = localData.withOrder(order);
    }

    ThreadUtils.runInAnotherThread(
        () -> {
          Log.info("zzz for order %d", order.getId());

          synchronized (localDataLock) {
            localData = localData.decrementBattery(10);
          }

          try {
            Thread.sleep(5 * 1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

          Log.userMessage(
              "%d: Delivered order %s. Sending confirmation message...",
              System.currentTimeMillis(), order);

          CityPoint droneStartingPosition = getLocalData().getPosition();
          double distanceTravelledForThisOrder =
              calculateTotalTravelledDistanceForOrder(droneStartingPosition, order);
          CompletedDeliveryMessage message =
              new CompletedDeliveryMessage(
                  System.currentTimeMillis(),
                  getIdentifier(),
                  order,
                  distanceTravelledForThisOrder,
                  pollutionTracker.readAllAndCleanAsDoubles(),
                  getLocalData().getBatteryPercentage());

          totalDeliveredOrders++;
          totalTravelledDistance += distanceTravelledForThisOrder;

          synchronized (localDataLock) {
            localData = localData.withoutOrder().moveTo(order.getDeliveryPoint());
          }

          Log.debug("deliverToMaster for ORDER_COMPLETITION");
          deliverToMaster(
              (master) -> middleware.notifyCompletedDelivery(master, message),
              () -> {
                synchronized (noOrderToDeliverCallbackLock) {
                  noOrderToDeliverCallbackLock.notifyAll();
                }
                currentState.afterCompletingAnOrder();
                Log.notice("Order %d was fully completed (from slave perspective)", order.getId());
              });
        });
    return true;
  }

  @Override
  public void onElectionMessage(DroneIdentifier candidateLeader, int candidateBatteryPercentage) {
    electionManager.onElectionMessage(candidateLeader, candidateBatteryPercentage);
  }

  @Override
  public void onElectedMessage(DroneIdentifier newLeader) {
    electionManager.onElectedMessage(newLeader);
  }

  @Override
  public void onCompletedDeliveryNotification(CompletedDeliveryMessage message) {
    currentState.onCompletedDeliveryNotification(message);
  }

  @Override
  public DroneData onDataRequest() {
    synchronized (localDataLock) {
      while (localData == null) {
        try {
          localDataLock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    return getLocalData();
  }

  @Override
  public void onLockRequest(int logicalClock, DroneIdentifier requester) {
    chargingAreaLock.onLockRequest(logicalClock, requester);
  }

  @Override
  public void onCompletedChargeMessage(DroneIdentifier sender) {
    currentState.onCompletedChargeMessage(sender);
  }

  public void becomeMaster() {
    if (currentState.isMaster()) {
      Log.warn("This drone was asked to become master, but it already is master!");
      return;
    }

    Log.notice("Becoming master");
    changeStateTo(
        new RingMasterState(
            this, store, middleware, adminServerClient, orderSource, electionManager));
  }

  private final Object noOrderToDeliverCallbackLock = new Object();

  /**
   * Takes a function that will be executed every time an order is completed and immediately if the
   * drone is not busy with an order. If the function returns <code>true</code> this callback will
   * be deleted and the function won't be called the next occasion, if the function returns <code>
   * false</code> it will be called again next time. The function will be executed in a different
   * thread, this method is not blocking.
   *
   * @param callback A function that will be called when there is no order to deliver, if this
   *     function returns <code>false</code> it will be called again the next time an order will be
   *     completed, otherwise it won't be called again until the next invocation of this method
   */
  public void doWhenThereIsNoOrderToDeliver(BooleanSupplier callback) {
    ThreadUtils.runInAnotherThread(
        () -> {
          boolean success;
          synchronized (noOrderToDeliverCallbackLock) {
            do {
              while (isDeliveringOrder()) {
                try {
                  noOrderToDeliverCallbackLock.wait();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
              success = callback.getAsBoolean();
            } while (!success);
          }
        });
  }
}
