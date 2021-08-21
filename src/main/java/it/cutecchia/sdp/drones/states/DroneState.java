package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;

public interface DroneState {
  void start();

  void teardown();

  void initiateShutdown();

  default void onCompletedDeliveryNotification(CompletedDeliveryMessage message) {
    Log.warn("Only the master should be notified of completed deliveries");
  }

  default void onDroneStatusUpdate(DroneIdentifier drone, DroneData updatedData) {
    Log.warn("Only the master should receive STATUS_UPDATES");
  }

  void afterCompletingAnOrder();

  default void onNewDroneJoin(DroneIdentifier newDrone) {}

  default boolean isMaster() {
    return false;
  }
}
