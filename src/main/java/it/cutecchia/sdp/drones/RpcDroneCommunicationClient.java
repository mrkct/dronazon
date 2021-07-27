package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.Optional;
import java.util.Set;

public class RpcDroneCommunicationClient implements DroneCommunicationClient {
  @Override
  public void broadcastDataRequest(Set<DroneIdentifier> drones) {}

  @Override
  public Optional<DroneIdentifier> requestToEnterRing(
      DroneIdentifier masterDrone, DroneData myDrone) {
    return Optional.empty();
  }
}
