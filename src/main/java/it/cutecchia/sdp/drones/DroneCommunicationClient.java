package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.Optional;

public interface DroneCommunicationClient {
  Optional<DroneJoinResponse> notifyDroneJoin(
      DroneIdentifier destination, DroneIdentifier sender, CityPoint startingPosition);

  interface AssignOrderCallback {
    void onFailure();

    void onOrderAccepted();
  }

  void assignOrder(Order order, DroneIdentifier drone, AssignOrderCallback callback);

  boolean notifyCompletedDelivery(DroneIdentifier masterDrone, CompletedDeliveryMessage message);

  interface DeliverToMasterCallback {
    boolean trySending(DroneIdentifier master);

    void onSuccess();
  }

  void deliverToMaster(DroneStore store, DeliverToMasterCallback callback);
}
