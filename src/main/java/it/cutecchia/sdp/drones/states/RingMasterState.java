package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.Drone;
import it.cutecchia.sdp.drones.DroneCommunicationClient;
import it.cutecchia.sdp.drones.OrderSource;
import it.cutecchia.sdp.drones.store.MasterDroneStore;

public class RingMasterState implements DroneState, OrderSource.OrderListener {
  private final Drone thisDrone;
  private final DroneCommunicationClient communicationClient;
  private final MasterDroneStore store;
  private final OrderSource orderSource;

  public RingMasterState(
      Drone drone,
      MasterDroneStore store,
      DroneCommunicationClient communicationClient,
      OrderSource orderSource) {
    this.thisDrone = drone;
    this.communicationClient = communicationClient;
    this.store = store;
    this.orderSource = orderSource;
  }

  @Override
  public void onOrderReceived(Order order) {
    Log.info("Assigning order %s...", order);
  }

  @Override
  public void start() {
    orderSource.start(this);
    communicationClient.broadcastDataRequest(store.getAllDroneIdentifiers());
  }

  @Override
  public void teardown() {
    orderSource.stop();
  }

  @Override
  public void shutdown() {
    orderSource.stop();
  }

  @Override
  public boolean isMaster() {
    return true;
  }
}
