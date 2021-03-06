package it.cutecchia.sdp.drones.states;

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

  default void onCompletedChargeMessage(DroneIdentifier drone) {
    Log.warn("Only the master should receive COMPLETED_CHARGE");
  }

  default void printStats() {}

  void afterCompletingAnOrder();

  default void onNewDroneJoin(DroneIdentifier newDrone) {}

  default boolean isMaster() {
    return false;
  }
}
