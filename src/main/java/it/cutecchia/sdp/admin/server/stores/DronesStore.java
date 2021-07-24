package it.cutecchia.sdp.admin.server.stores;

import it.cutecchia.sdp.admin.server.beans.DroneInfo;
import java.util.Set;

public interface DronesStore {
  class DroneIdAlreadyInUse extends Exception {}

  class DroneIdNotFound extends Exception {}

  DroneInfo addNewDrone(int droneId, String ipAddress, int connectionPort)
      throws DroneIdAlreadyInUse;

  void removeDroneById(int droneId) throws DroneIdNotFound;

  Set<DroneInfo> getRegisteredDrones();
}
