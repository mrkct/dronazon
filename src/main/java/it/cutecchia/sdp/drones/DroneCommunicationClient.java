package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import java.util.Optional;
import java.util.Set;

public interface DroneCommunicationClient {
  default void broadcastDataRequest(Set<DroneIdentifier> drones) {}

  Optional<DroneJoinResponse> notifyDroneJoin(
      DroneIdentifier destination, DroneIdentifier sender, CityPoint startingPosition);
}
