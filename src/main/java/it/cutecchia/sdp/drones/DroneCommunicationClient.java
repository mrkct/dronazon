package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.Optional;
import java.util.Set;

public interface DroneCommunicationClient {
  void broadcastDataRequest(Set<DroneIdentifier> drones);

  Optional<DroneIdentifier> requestToEnterRing(DroneIdentifier masterDrone, DroneData myDrone);
}
