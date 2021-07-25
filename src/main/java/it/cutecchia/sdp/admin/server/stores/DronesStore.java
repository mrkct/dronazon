package it.cutecchia.sdp.admin.server.stores;

import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.Set;

public interface DronesStore {
  class DroneIdAlreadyInUse extends Exception {}

  class DroneIdNotFound extends Exception {}

  void addNewDrone(int droneId, String ipAddress, int connectionPort) throws DroneIdAlreadyInUse;

  void removeDroneById(int droneId) throws DroneIdNotFound;

  Set<DroneIdentifier> getRegisteredDrones();
}
