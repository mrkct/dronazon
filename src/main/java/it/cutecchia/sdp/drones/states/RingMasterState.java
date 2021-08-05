package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.Drone;
import it.cutecchia.sdp.drones.DroneCommunicationClient;
import it.cutecchia.sdp.drones.OrderAssigner;
import it.cutecchia.sdp.drones.OrderSource;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

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
    Log.info("Master is requesting data from every other drone");
    Set<DroneIdentifier> drones = new TreeSet<>(store.getAllDroneIdentifiers());
    drones.parallelStream()
        .forEach(
            (destination) -> {
              Optional<DroneData> data = communicationClient.requestData(destination);
              Log.info(
                  "requestData from %s -> %s",
                  destination,
                  data.map(droneData -> "success, " + droneData.toString()).orElse("failed"));
              if (data.isPresent()) {
                store.handleDroneUpdateData(destination, data.get());
              } else {
                store.signalFailedCommunicationWithDrone(destination);
              }
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
