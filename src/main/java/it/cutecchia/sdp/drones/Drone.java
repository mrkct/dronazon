package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.drones.states.DroneState;
import it.cutecchia.sdp.drones.states.StartupState;

public class Drone {
  private final long droneId;
  private final int listenPort;
  private final String adminServerAddress;
  private final int adminServerPort;

  private DroneState currentState;

  public Drone(long droneId, int listenPort, String adminServerAddress, int adminServerPort) {
    this.droneId = droneId;
    this.listenPort = listenPort;
    this.adminServerAddress = adminServerAddress;
    this.adminServerPort = adminServerPort;

    this.currentState = new StartupState(this);
  }

  public void start() {
    this.currentState.start();
  }

  public void shutdown() {
    this.currentState.shutdown();
  }
}
