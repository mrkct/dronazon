package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.drones.Drone;

public class RingMasterState implements DroneState {
  private final Drone drone;

  public RingMasterState(Drone drone) {
    this.drone = drone;
  }

  @Override
  public void start() {}

  @Override
  public void teardown() {}

  @Override
  public void shutdown() {}

  @Override
  public boolean isMaster() {
    return true;
  }
}
