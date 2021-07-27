package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.drones.states.DroneState;
import it.cutecchia.sdp.drones.states.EnteringRingState;
import it.cutecchia.sdp.drones.states.RingMasterState;
import it.cutecchia.sdp.drones.states.StartupState;
import java.util.Set;

public class Drone {

  private final AdminServerClient adminServerClient;
  private final DroneIdentifier identifier;

  private DroneData info;
  private DroneState currentState;

  public Drone(DroneIdentifier identifier, AdminServerClient adminServerClient) {
    this.identifier = identifier;
    this.adminServerClient = adminServerClient;
    this.currentState = new StartupState(this, adminServerClient);
  }

  public DroneIdentifier getIdentifier() {
    return identifier;
  }

  public void start() {
    // TODO: Start RPC server?
    this.currentState.start();
  }

  public void shutdown() {
    // TODO: Stop RPC server?
    this.currentState.shutdown();
  }

  public void changeStateTo(DroneState newState) {
    this.currentState.teardown();
    this.currentState = newState;
    this.currentState.start();
  }

  public void onAdminServerAcceptance(CityPoint position, Set<DroneIdentifier> allDrones) {
    System.out.printf("Drone %d# was accepted by the admin server%n", identifier.getId());
    info = new DroneData(position);
    if (allDrones.size() == 1) {
      changeStateTo(new RingMasterState(this, allDrones, null));
    } else {
      changeStateTo(new EnteringRingState(this, allDrones));
    }
  }
}
