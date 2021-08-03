package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;

// FIXME: Rename into something better (somethingService?)
public interface DroneCommunicationServer {
  DroneJoinResponse onDroneJoin(DroneIdentifier identifier, CityPoint startingPosition);

  void onOrderAssigned(Order order);

  void onCompletedDeliveryNotification(CompletedDeliveryMessage message);

  // Drone.DroneJoinResponse onDroneJoin(Drone.DroneJoinMessage message);

  // void onOrderCompletion();
  // PollutionData onPollutionDataRequest();
  // void onElectionMessageReceived(ElectionMessage message);
}
