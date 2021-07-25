package it.cutecchia.sdp.drones.states;

public interface DroneState {
  void start();

  void teardown();

  void shutdown();

  default boolean isMaster() {
    return false;
  }
}
