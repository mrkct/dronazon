package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.admin.server.messages.DroneEnterResponse;
import it.cutecchia.sdp.drones.Drone;

/**
 * In this state the drone communicates with the admin server to get the necessary info to join the
 * ring of drones
 */
public class StartupState implements DroneState {
  private final Drone drone;
  private final AdminServerClient client;

  public StartupState(Drone drone, AdminServerClient client) {
    this.drone = drone;
    this.client = client;
  }

  @Override
  public void start() {
    try {
      DroneEnterResponse response = client.requestDroneToEnter(drone.getIdentifier());
      drone.onAdminServerAcceptance(response.getNewlyAddedDronePosition(), response.getAllDrones());
    } catch (AdminServerClient.DroneIdAlreadyInUse e) {
      // FIXME: Change state to Fatal?
      System.err.println("Oh no: id already in use");
    }
  }

  @Override
  public void teardown() {}

  @Override
  public void shutdown() {}
}
