package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.drones.Drone;
import it.cutecchia.sdp.drones.DroneCommunicationClient;
import it.cutecchia.sdp.drones.InMemoryVirtualDroneStore;
import it.cutecchia.sdp.drones.VirtualDroneStore;
import java.util.Set;

public class RingMasterState implements DroneState, VirtualDroneStore.OnUpdateReceivedListener {
  private final Drone thisDrone;
  private final DroneCommunicationClient communicationClient;
  private final VirtualDroneStore virtualDroneStore;

  public RingMasterState(
      Drone drone, Set<DroneIdentifier> allDrones, DroneCommunicationClient communicationClient) {
    this.thisDrone = drone;
    this.virtualDroneStore = new InMemoryVirtualDroneStore(allDrones);
    this.communicationClient = communicationClient;
  }

  @Override
  public void onBroadcastInfoRequestSuccess() {}

  @Override
  public void onBroadcastInfoRequestFailure(Set<DroneData> failedCommunications) {}

  @Override
  public void start() {
    virtualDroneStore.broadcastUpdateInfoRequest(communicationClient, this);
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
