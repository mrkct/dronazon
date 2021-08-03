package it.cutecchia.sdp.drones.store;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Order;
import java.util.Optional;
import java.util.Set;

public interface DroneStore {
  Set<DroneIdentifier> getAllDroneIdentifiers();

  void addDrone(DroneIdentifier identifier);

  void handleDroneUpdateData(DroneIdentifier identifier, DroneData data);

  Optional<DroneData> getDroneData(DroneIdentifier identifier);

  Optional<DroneIdentifier> getNextDroneInElectionRing(DroneIdentifier identifier);

  void signalFailedCommunicationWithDrone(DroneIdentifier drone);

  void signalDroneWasAssignedOrder(DroneIdentifier drone, Order order);

  void setKnownMaster(DroneIdentifier drone);

  DroneIdentifier getKnownMaster();
}
