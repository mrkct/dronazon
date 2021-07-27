package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.Set;
import java.util.stream.Collectors;

public class InMemoryVirtualDroneStore implements VirtualDroneStore {
  private Set<VirtualDrone> drones;

  public InMemoryVirtualDroneStore(Set<DroneIdentifier> drones) {
    this.drones =
        drones.parallelStream().map(id -> new VirtualDrone(id, null)).collect(Collectors.toSet());
  }

  @Override
  public void broadcastUpdateInfoRequest(
      DroneCommunicationClient communicationClient, OnUpdateReceivedListener listener) {}

  @Override
  public Set<VirtualDrone> getAllAvailableDronesInfo() {
    return null;
  }

  @Override
  public void updateDroneInfo(DroneData info) {}
}
