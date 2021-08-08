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

public class Drone implements DroneCommunicationServer {
  private final AdminServerClient adminServerClient;
  private final DroneIdentifier identifier;
  private final OrderSource orderSource;
  private final RpcDroneCommunicationMiddleware middleware;
  private final DroneStore store = new InMemoryDroneStore();
  private final PollutionTracker pollutionTracker = new PollutionTracker();

  private DroneData data;
  private DroneState currentState;
  private volatile boolean deliveringOrder = false;

  public Drone(
      DroneIdentifier identifier, OrderSource orderSource, AdminServerClient adminServerClient) {
    this.identifier = identifier;
    this.adminServerClient = adminServerClient;
    this.currentState = new StartupState(this, adminServerClient);
    this.middleware = new RpcDroneCommunicationMiddleware(identifier, this);
    this.orderSource = orderSource;
  }

  public DroneIdentifier getIdentifier() {
    return identifier;
  }

  public DroneData getData() {
    return data;
  }

  public boolean isDeliveringOrder() {
    return deliveringOrder;
  }

  public void start() throws IOException {
    middleware.startRpcServer();
    currentState.start();
    pollutionTracker.startTracking();
  }

  public void shutdown() {
    currentState.shutdown();
  }

  public void changeStateTo(DroneState newState) {
    currentState.teardown();
    currentState = newState;
    currentState.start();
  }

  public void onAdminServerAcceptance(CityPoint position, Set<DroneIdentifier> allDrones) {
    Log.notice("Drone %d# was accepted by the admin server", identifier.getId());

    data = new DroneData(position);
    allDrones.forEach(store::addDrone);

    if (allDrones.size() == 1) {
      store.handleDroneUpdateData(identifier, data);
      store.setKnownMaster(identifier);
      changeStateTo(new RingMasterState(this, store, middleware, adminServerClient, orderSource));
    } else {
      changeStateTo(new RingSlaveState(this, store, middleware, adminServerClient));
    }
  }

  @Override
  public DroneJoinResponse onDroneJoin(DroneIdentifier identifier, CityPoint startingPosition) {
    store.addDrone(identifier);
    store.handleDroneUpdateData(identifier, new DroneData(startingPosition));
    Log.notice("A new drone (#%d) joined the ring at %s", identifier.getId(), startingPosition);

    return new DroneJoinResponse(getIdentifier(), currentState.isMaster());
  }

  @Override
  public void onOrderAssigned(Order order) {
    Log.info("%d: I was assigned order %s. Delivering...", System.currentTimeMillis(), order);
    assert !deliveringOrder;
    new Thread(
            () -> {
              // FIXME: Why?
              synchronized (this) {
                deliveringOrder = true;
              }

              try {
                Thread.sleep(5 * 1000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }

              Log.info(
                  "%d: Delivered order %s. Sending confirmation message...",
                  System.currentTimeMillis(), order);

              CompletedDeliveryMessage message =
                  new CompletedDeliveryMessage(
                      System.currentTimeMillis(),
                      getIdentifier(),
                      order,
                      data.getPosition().distanceTo(order.getStartPoint())
                          + order.getStartPoint().distanceTo(order.getDeliveryPoint()),
                      pollutionTracker.getAverageMeasurementsValue(),
                      getData().getBatteryPercentage() - 10);

              pollutionTracker.clearAllMeasurements();
              deliveringOrder = false;

              data = new DroneData(order.getDeliveryPoint(), data.getBatteryPercentage() - 10);

              middleware.deliverToMaster(
                  store, (master) -> middleware.notifyCompletedDelivery(master, message));

              if (data.getBatteryPercentage() < 15) {
                currentState.onLowBattery();
              }
              currentState.afterCompletingAnOrder();
            })
        .start();
  }

  @Override
  public void onCompletedDeliveryNotification(CompletedDeliveryMessage message) {
    currentState.onCompletedDeliveryNotification(message);
  }

  @Override
  public DroneData onDataRequest() {
    return data;
  }
}
