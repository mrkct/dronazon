package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.drones.Drone;

public class StartupState implements DroneState {
  private final Drone drone;

  public StartupState(Drone drone) {
    this.drone = drone;
  }

  @Override
  public void start() {}

  @Override
  public void shutdown() {}
}
