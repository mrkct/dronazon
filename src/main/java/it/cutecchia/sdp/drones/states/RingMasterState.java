package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.*;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class RingMasterState implements DroneState, OrderSource.OrderListener {
  private final Drone drone;
  private final DroneCommunicationClient communicationClient;
  private final DroneStore store;
  private final OrderSource orderSource;
  private final OrderAssigner orderAssigner;
  private final FleetStatsTracker statsTracker;
  private final AdminServerClient adminServerClient;

  public RingMasterState(
      Drone drone,
      DroneStore store,
      DroneCommunicationClient communicationClient,
      AdminServerClient client,
      OrderSource orderSource) {
    this.drone = drone;
    this.communicationClient = communicationClient;
    this.store = store;
    this.orderSource = orderSource;
    this.orderAssigner = new OrderAssigner(store, communicationClient);
    this.statsTracker = new FleetStatsTracker(store, client);
    this.adminServerClient = client;
  }

  @Override
  public void onOrderReceived(Order order) {
    Log.info("Received order %s", order);
    orderAssigner.enqueueOrder(order);
  }

  @Override
  public void onNewDroneJoin(DroneIdentifier newDrone) {
    orderAssigner.notifyNewDroneJoined();
  }

  @Override
  public void onCompletedDeliveryNotification(CompletedDeliveryMessage message) {
    Log.info("Order %s was completed", message.getOrder());
    store.handleDroneUpdateData(
        message.getDrone(),
        new DroneData(message.getOrder().getDeliveryPoint(), message.getBatteryPercentage()));
    orderAssigner.notifyOrderCompleted(message.getOrder());
    statsTracker.handleCompletedDeliveryStats(
        message.getPollutionValues(), message.getTravelledKms());
  }

  @Override
  public void start() {
    Log.notice("Master is requesting data from every other drone");
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
    statsTracker.start();
    /**
     * Cheap fix: If we started the orderSource before sending the requestData then what could
     * happend is that we send a data request and a response with order=null is queued up in grpc,
     * except we also get an order first. We assign an order to that drone, process the data request
     * response (resetting assignedOrder to null) and assign another drone again! By putting
     * orderSource.start after the data request is over we avoid the problem because we are sure the
     * drone has 0 pending orders and nothing will be assigned until we receive the first order
     */
    orderSource.start(this);
  }

  @Override
  public void teardown() {
    throw new IllegalStateException(
        "A master drone should never be able to change to a different state");
  }

  @Override
  public void initiateShutdown() {
    Log.notice("Master drone initiated the shutdown procedure");

    doAsSoonAsThereIsNoOrderToDeliver(
        () -> {
          Log.notice("Shutdown - No order to deliver");
          synchronized (drone) {
            if (drone.isDeliveringOrder()) {
              Log.notice("Unlucky, got a new one assigned just now");
              return;
            }
            Log.notice("Order source was stopped. Waiting for all pending orders to be assigned");
            orderSource.stop();
          }
          orderAssigner.doAsSoonAsThereAreNoPendingOrders(
              () -> {
                Log.notice("No more pending orders, waiting to deliver the very last order");
                doAsSoonAsThereIsNoOrderToDeliver(
                    () -> {
                      synchronized (drone) {
                        if (drone.isDeliveringOrder()) {
                          Log.notice("Delivering the very last order");
                          return;
                        }
                        Log.notice("Final shutdown phase - Closing all. Goodbye :)");
                        communicationClient.shutdown();
                        statsTracker.sendStatsAndShutdown();
                        adminServerClient.requestDroneExit(drone.getIdentifier());
                        System.exit(0);
                      }
                    });
                Log.notice("Second lambda - end");
              });
          Log.notice("First lambda - end");
        });
    Log.notice("initiateShutdown - end");
  }

  private Runnable noOrderToDeliverCallback = null;

  private void doAsSoonAsThereIsNoOrderToDeliver(Runnable callback) {
    noOrderToDeliverCallback = callback;
    synchronized (drone) {
      if (!drone.isDeliveringOrder()) {
        Log.notice("shortcut for asSoonAsThereIsNoOrderToDeliver");
        noOrderToDeliverCallback.run();
      }
    }
  }

  public void afterCompletingAnOrder() {
    if (noOrderToDeliverCallback != null) {
      noOrderToDeliverCallback.run();
    }
    if (drone.getData().isLowBattery()) initiateShutdown();
  }

  @Override
  public boolean isMaster() {
    return true;
  }
}
