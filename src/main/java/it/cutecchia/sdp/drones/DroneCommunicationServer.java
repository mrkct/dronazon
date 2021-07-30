package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;

// FIXME: Rename into something better (somethingService?)
public interface DroneCommunicationServer {
  DroneJoinResponse onDroneJoin(DroneIdentifier identifier, CityPoint startingPosition);

  // Drone.DroneJoinResponse onDroneJoin(Drone.DroneJoinMessage message);

  // void onOrderAssignment(Order order);
  // void onOrderCompletion();
  // PollutionData onPollutionDataRequest();
  // void onElectionMessageReceived(ElectionMessage message);
}
