package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class OrderAssigner {
  private final DroneStore dronesStore;
  private final DroneCommunicationClient communicationClient;
  private final List<Order> pendingOrders = new ArrayList<>();

  public OrderAssigner(
      @Nonnull DroneStore dronesStore, @Nonnull DroneCommunicationClient communicationClient) {
    this.dronesStore = dronesStore;
    this.communicationClient = communicationClient;
  }

  public void enqueueOrder(Order order) {
    synchronized (pendingOrders) {
      pendingOrders.add(order);
    }
    assignAllPossibleOrders();
  }

  public void notifyOrderCompleted(Order order) {
    assignAllPossibleOrders();
  }

  private DroneIdentifier findBestDroneForOrder(Set<DroneIdentifier> drones, Order order) {
    assert (!drones.isEmpty());

    return drones.stream()
        .reduce(
            (left, right) -> {
              DroneData leftData = dronesStore.getDroneData(left).orElse(null);
              DroneData rightData = dronesStore.getDroneData(right).orElse(null);

              if (leftData == null && rightData == null) {
                return left.getId() > right.getId() ? left : right;
              } else if (leftData == null) {
                return right;
              } else if (rightData == null) {
                return left;
              }

              double leftDistance = order.getStartPoint().distanceTo(leftData.getPosition());
              double rightDistance = order.getStartPoint().distanceTo(rightData.getPosition());

              if (leftDistance != rightDistance) {
                return leftDistance < rightDistance ? left : right;
              }

              int leftBattery = leftData.getBatteryPercentage();
              int rightBattery = rightData.getBatteryPercentage();
              if (leftBattery != rightBattery) {
                return leftBattery > rightBattery ? left : right;
              }

              return left.getId() > right.getId() ? left : right;
            })
        .get();
  }

  private Set<DroneIdentifier> getAvailableDronesForDeliveries() {
    return dronesStore.getAllDroneIdentifiers().parallelStream()
        .filter(
            id -> {
              Optional<DroneData> data = dronesStore.getDroneData(id);
              Log.info("\t#%d -> %s", id.getId(), data.orElse(null));
              return data.isPresent() && data.get().getAssignedOrder() == null;
            })
        .collect(Collectors.toSet());
  }

  private synchronized void assignAllPossibleOrders() {
    Log.info("OrderAssigner: Assigning all I can");
    Set<DroneIdentifier> drones = getAvailableDronesForDeliveries();
    Log.info(
        "OrderAssigner: %d available drones for %d pending orders",
        drones.size(), pendingOrders.size());

    while (!drones.isEmpty() && !pendingOrders.isEmpty()) {
      Order order = pendingOrders.remove(0);
      DroneIdentifier drone = findBestDroneForOrder(drones, order);
      drones.remove(drone);
      communicationClient.assignOrder(
          order,
          drone,
          new DroneCommunicationClient.AssignOrderCallback() {
            @Override
            public void onFailure() {
              Log.warn(
                  "Failed to assign order %d to drone #%d. Re-adding order to queue...",
                  order.getId(), drone.getId());
              dronesStore.signalFailedCommunicationWithDrone(drone);
              enqueueOrder(order);
            }

            @Override
            public void onOrderAccepted() {
              Log.info("Order %d was accepted by drone #%d", order.getId(), drone.getId());
              dronesStore.signalDroneWasAssignedOrder(drone, order);
            }
          });
    }

    Log.info(
        "OrderAssigner: Completed. Available drones: %d, pending orders: %d",
        drones.size(), pendingOrders.size());
  }
}
