package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.*;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import it.cutecchia.sdp.drones.states.DroneState;
import it.cutecchia.sdp.drones.states.EnteringRingState;
import it.cutecchia.sdp.drones.states.RingMasterState;
import it.cutecchia.sdp.drones.states.StartupState;
import it.cutecchia.sdp.drones.store.DroneStore;
import it.cutecchia.sdp.drones.store.MasterDroneStore;
import it.cutecchia.sdp.drones.store.SlaveDroneStore;
import java.io.IOException;
import java.util.Set;

public class Drone implements DroneCommunicationServer {
  private final AdminServerClient adminServerClient;
  private final DroneIdentifier identifier;
  private final OrderSource orderSource;
  private final RpcDroneCommunicationMiddleware middleware;
  private DroneStore store;

  private DroneData data;
  private DroneState currentState;

  public Drone(
      DroneIdentifier identifier, OrderSource orderSource, AdminServerClient adminServerClient) {
    this.identifier = identifier;
    this.adminServerClient = adminServerClient;
    this.currentState = new StartupState(this, adminServerClient);
    this.middleware = new RpcDroneCommunicationMiddleware(identifier, this);
    this.store = new SlaveDroneStore();
    this.orderSource = orderSource;
  }

  public DroneIdentifier getIdentifier() {
    return identifier;
  }

  public DroneData getData() {
    return data;
  }

  public void start() throws IOException {
    this.middleware.startRpcServer();
    this.currentState.start();
  }

  public void shutdown() {
    this.middleware.shutdownRpcServer();
    this.currentState.shutdown();
  }

  public void changeStateTo(DroneState newState) {
    this.currentState.teardown();
    this.currentState = newState;
    this.currentState.start();
  }

  public void onAdminServerAcceptance(CityPoint position, Set<DroneIdentifier> allDrones) {
    Log.info("Drone %d# was accepted by the admin server", identifier.getId());

    data = new DroneData(position);
    allDrones.forEach(drone -> store.addDrone(drone));

    if (allDrones.size() == 1) {
      MasterDroneStore masterDroneStore = new MasterDroneStore(store);
      store = masterDroneStore;
      changeStateTo(new RingMasterState(this, masterDroneStore, middleware, orderSource));
    } else {
      store = new SlaveDroneStore(store);
      changeStateTo(new EnteringRingState(this, store, middleware));
    }
  }

  @Override
  public DroneJoinResponse onDroneJoin(DroneIdentifier identifier, CityPoint startingPosition) {
    store.addDrone(identifier);
    store.handleDroneUpdateData(identifier, new DroneData(startingPosition));

    return new DroneJoinResponse(getIdentifier(), currentState.isMaster());
  }
}
