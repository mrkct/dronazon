package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;

public interface DroneState {
  void start();

  void teardown();

  void shutdown();

  void onLowBattery();

  default void onCompletedDeliveryNotification(CompletedDeliveryMessage message) {
    Log.warn(
        "A non-master drone received an order completed message (Order: %s, Drone: %s)",
        message.getOrder(), message.getDrone());
  }

  void afterCompletingAnOrder();

  default boolean isMaster() {
    return false;
  }
}
