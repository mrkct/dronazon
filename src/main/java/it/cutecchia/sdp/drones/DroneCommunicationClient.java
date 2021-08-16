package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import java.util.Optional;

public interface DroneCommunicationClient {
  boolean requestHeartbeat(DroneIdentifier destination);

  Optional<DroneJoinResponse> notifyDroneJoin(
      DroneIdentifier destination, CityPoint startingPosition);

  boolean assignOrder(Order order, DroneIdentifier drone);

  boolean notifyCompletedDelivery(DroneIdentifier masterDrone, CompletedDeliveryMessage message);

  interface DeliverToMasterCallback {
    boolean trySending(DroneIdentifier master);
  }

  Optional<DroneData> requestData(DroneIdentifier drone);

  boolean forwardElectionMessage(
      DroneIdentifier destination,
      DroneIdentifier candidateLeader,
      int candidateLeaderBatteryPercentage);

  boolean forwardElectedMessage(DroneIdentifier destination, DroneIdentifier newLeader);

  void shutdown();
}
