package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.drones.Drone;
import java.util.Set;

public class EnteringRingState implements DroneState {
  public EnteringRingState(Drone drone, Set<DroneIdentifier> allDrones) {}

  @Override
  public void start() {}

  @Override
  public void teardown() {}

  @Override
  public void shutdown() {}
}
