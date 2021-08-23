package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.admin.server.messages.DroneEnterResponse;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.drones.Drone;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;

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
  public void onCompletedDeliveryNotification(CompletedDeliveryMessage message) {
    throw new IllegalStateException("A drone in StartupState was notified of a completed delivery");
  }

  @Override
  public boolean isMaster() {
    return false;
  }

  @Override
  public void afterCompletingAnOrder() {
    throw new IllegalStateException();
  }

  @Override
  public void start() {
    try {
      DroneEnterResponse response = client.requestDroneToEnter(drone.getIdentifier());
      drone.onAdminServerAcceptance(response.getNewlyAddedDronePosition(), response.getAllDrones());
    } catch (AdminServerClient.DroneIdAlreadyInUse e) {
      Log.userMessage(
          "Drone id #%d is already in use on the server", drone.getIdentifier().getId());
      Log.userMessage("Choose a different id and restart the drone");
      System.exit(-1);
    }
  }

  @Override
  public void teardown() {}

  @Override
  public void initiateShutdown() {}
}
