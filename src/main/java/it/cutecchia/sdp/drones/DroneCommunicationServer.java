package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;

public interface DroneCommunicationServer {
  DroneJoinResponse onDroneJoin(DroneIdentifier identifier, CityPoint startingPosition);

  boolean onOrderAssigned(Order order);

  void onCompletedDeliveryNotification(CompletedDeliveryMessage message);

  DroneData onDataRequest();

  void onElectionMessage(DroneIdentifier candidateLeader, int candidateBatteryPercentage);

  void onElectedMessage(DroneIdentifier newLeader);

  void onLockRequest(int logicalClock, DroneIdentifier requester);

  void onCompletedChargeMessage(DroneIdentifier sender);
}
