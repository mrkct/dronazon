package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.Drone;
import it.cutecchia.sdp.drones.DroneCommunicationClient;
import it.cutecchia.sdp.drones.OrderAssigner;
import it.cutecchia.sdp.drones.OrderSource;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.store.DroneStore;

public class RingMasterState implements DroneState, OrderSource.OrderListener {
  private final Drone thisDrone;
  private final DroneCommunicationClient communicationClient;
  private final DroneStore store;
  private final OrderSource orderSource;
  private final OrderAssigner orderAssigner;

  public RingMasterState(
      Drone drone,
      DroneStore store,
      DroneCommunicationClient communicationClient,
      OrderSource orderSource) {
    this.thisDrone = drone;
    this.communicationClient = communicationClient;
    this.store = store;
    this.orderSource = orderSource;
    this.orderAssigner = new OrderAssigner(store, communicationClient);
  }

  @Override
  public void onOrderReceived(Order order) {
    Log.info("Received order %s", order);
    orderAssigner.enqueueOrder(order);
  }

  @Override
  public void onCompletedDeliveryNotification(CompletedDeliveryMessage message) {
    Log.info("(RingMasterState): Order %s was completed", message.getOrder());
    orderAssigner.notifyOrderCompleted(message.getOrder());
    store.handleDroneUpdateData(
        message.getDrone(),
        new DroneData(message.getOrder().getDeliveryPoint(), message.getBatteryPercentage()));
    // FIXME: Handle the statistics
  }

  @Override
  public void start() {
    orderSource.start(this);
    store.getAllDroneIdentifiers().parallelStream()
        .forEach(
            (destination) -> {
              /*
              Optional<DroneData> data = communicationClient.requestData(destination);
              if (!data.isPresent()) {
                store.signalFailedCommunicationWithDrone(destination);
                return;
              }
              store.addDrone(destination);
              store.handleDroneUpdateData(destination, data.get());*/
            });
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
  public void onLowBattery() {
    Log.info("Drone reached low battery");
  }

  @Override
  public boolean isMaster() {
    return true;
  }
}
