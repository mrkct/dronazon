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
              "%d#: Total delivered orders: %d\tDistance travelled: %3f\tRemaining battery: %d%% Master? %s%",
              identifier.getId(),
              totalDeliveredOrders,
              totalTravelledDistance,
              getLocalData().getBatteryPercentage(),
              currentState.isMaster() ? "yes" : "no");
        }
      };

  private final Timer masterHeartbeatTimer = new Timer();
  private final TimerTask masterHeartbeat =
      new TimerTask() {
        @Override
        public void run() {
          Log.notice("Deliver to master because of heartbeat");
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

  public void start() throws IOException {
    middleware.startRpcServer();
    currentState.start();
    pollutionTracker.startTracking();
  }

  public void printStats() {
    printStatsTask.run();
  }

  private volatile boolean shutdownInitiated = false;

  public synchronized void shutdown() {
    if (shutdownInitiated) return;
    shutdownInitiated = true;
    currentState.initiateShutdown();
  }

  public synchronized void recharge() {
    assert !chargingAreaLock.isOwned();
    assert !shutdownInitiated;
    new Thread(
            () -> {
              Log.userMessage("Waiting to get permission to recharge from all other drones...");
              chargingAreaLock.take();
              DataRaceTester.sleep();
              Log.debug("Took the lock!");

              DataRaceTester.sleep();

              Log.userMessage("I will recharge as soon as I complete my order");
              doWhenThereIsNoOrderToDeliver(
                  () -> {
                    synchronized (this) {
                      if (isDeliveringOrder()) {
                        return false;
                      }

                      synchronized (localDataLock) {
                        localData = localData.startRecharging();
                      }
                    }

                    Log.userMessage(
                        "%d: Starting the recharge. Sleeping...", System.currentTimeMillis());
                    try {
                      assert chargingAreaLock.isOwned();
                      assert !isDeliveringOrder();

                      try {
                        Thread.sleep(TIME_TO_RECHARGE);
                      } catch (InterruptedException e) {
                        e.printStackTrace();
                      }
                    } finally {
                      chargingAreaLock.release();
                      Log.userMessage("%d: I have finished recharging", System.currentTimeMillis());
                      DataRaceTester.sleep();

                      localData = new DroneData(new CityPoint(0, 0), 100);

                      deliverToMaster(
                          (master) -> {
                            Log.debug("Deliver to master because of status update (lock release)");
                            return middleware.notifyStatusUpdate(
                                master, identifier, getLocalData());
                          },
                          () -> {
                            Log.debug("Notified that I'm done recharging to master");
                          });
                    }
                    return true;
                  });
            })
        .start();
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
      changeStateTo(new RingMasterState(this, store, middleware, adminServerClient, orderSource));
    } else {
      changeStateTo(new RingSlaveState(this, store, middleware, adminServerClient));
    }
    masterHeartbeatTimer.scheduleAtFixedRate(masterHeartbeat, 0, MASTER_HEARTBEAT_PERIOD);
    printStatsTimer.scheduleAtFixedRate(printStatsTask, 0, PRINT_STATS_PERIOD);
  }

  @Override
  public DroneJoinResponse onDroneJoin(DroneIdentifier identifier, CityPoint startingPosition) {
    store.addDrone(identifier);
    DataRaceTester.sleep();
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

    electionManager.setOnNewMasterElectedListener(
        () -> {
          if (send.apply(store.getKnownMaster())) {
            electionManager.clearOnNewMasterElectedListener();
            onSuccess.run();
          } else {
            electionManager.beginElection();
          }
        });
    electionManager.beginElection();
  }

  @Override
  public synchronized boolean onOrderAssigned(Order order) {
    if (chargingAreaLock.isOwned() || localData.isRecharging()) {
      Log.info("Refusing to deliver order %s because I'm recharging", order.getId());
      return false;
    }

    Log.userMessage(
        "%d: I was assigned order %s. Delivering...", System.currentTimeMillis(), order);
    assert !isDeliveringOrder();

    synchronized (localDataLock) {
      localData = localData.withOrder(order);
    }

    new Thread(
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

              deliverToMaster(
                  (master) -> {
                    Log.debug("deliverToMaster because of order completion");
                    return middleware.notifyCompletedDelivery(master, message);
                  },
                  () -> {
                    if (noOrderToDeliverCallback != null) {
                      runNoOrderToDeliverCallbackInAnotherThread();
                    }
                    currentState.afterCompletingAnOrder();
                    Log.notice(
                        "Order %d was fully completed (from slave perspective)", order.getId());
                  });
            })
        .start();
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
  public void onStatusUpdate(DroneIdentifier sender, DroneData updatedData) {
    currentState.onDroneStatusUpdate(sender, updatedData);
  }

  public void becomeMaster() {
    Log.notice("Becoming master");
    changeStateTo(new RingMasterState(this, store, middleware, adminServerClient, orderSource));
  }

  private final Object noOrderToDeliverCallbackLock = new Object();
  private BooleanSupplier noOrderToDeliverCallback = null;

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
    synchronized (noOrderToDeliverCallbackLock) {
      noOrderToDeliverCallback = callback;
      if (!isDeliveringOrder()) {
        Log.debug("shortcut for asSoonAsThereIsNoOrderToDeliver");
        runNoOrderToDeliverCallbackInAnotherThread();
      }
    }
  }

  private void runNoOrderToDeliverCallbackInAnotherThread() {
    assert noOrderToDeliverCallback != null;
    new Thread(
            () -> {
              Log.info("noOrderToDeliverCallback started");
              // The callback might set another callback inside it and we don't want to delete that
              final BooleanSupplier callback = noOrderToDeliverCallback;
              if (noOrderToDeliverCallback.getAsBoolean() && callback == noOrderToDeliverCallback) {
                noOrderToDeliverCallback = null;
              }
            })
        .start();
  }
}
