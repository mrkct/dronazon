package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.drones.Drone;
import it.cutecchia.sdp.drones.DroneCommunicationClient;
import it.cutecchia.sdp.drones.store.MasterDroneStore;

public class RingMasterState implements DroneState {
  private final Drone thisDrone;
  private final DroneCommunicationClient communicationClient;
  private final MasterDroneStore store;

  public RingMasterState(
      Drone drone, MasterDroneStore store, DroneCommunicationClient communicationClient) {
    this.thisDrone = drone;
    this.communicationClient = communicationClient;
    this.store = store;
  }

  @Override
  public void start() {
    communicationClient.broadcastDataRequest(store.getAllDroneIdentifiers());
  }

  @Override
  public void teardown() {}

  @Override
  public void shutdown() {}

  @Override
  public boolean isMaster() {
    return true;
  }
}
