package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.Optional;

public interface DroneCommunicationClient {
  Optional<DroneJoinResponse> notifyDroneJoin(
      DroneIdentifier destination, CityPoint startingPosition);

  boolean assignOrder(Order order, DroneIdentifier drone);

  boolean notifyCompletedDelivery(DroneIdentifier masterDrone, CompletedDeliveryMessage message);

  interface DeliverToMasterCallback {
    boolean trySending(DroneIdentifier master);
  }

  void deliverToMaster(DroneStore store, DeliverToMasterCallback callback);

  Optional<DroneData> requestData(DroneIdentifier drone);

  void shutdown();
}
