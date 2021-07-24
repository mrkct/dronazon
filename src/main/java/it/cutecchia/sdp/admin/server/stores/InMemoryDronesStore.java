package it.cutecchia.sdp.admin.server.stores;

import it.cutecchia.sdp.admin.server.beans.DroneInfo;
import it.cutecchia.sdp.common.CityPoint;
import java.util.*;
import javax.annotation.Nonnull;

public class InMemoryDronesStore implements DronesStore {
  private static DronesStore instance = null;

  public static DronesStore getInstance() {
    if (instance == null) {
      instance = new InMemoryDronesStore();
    }
    return instance;
  }

  private final Set<DroneInfo> drones = new HashSet<>();
  private final Random random = new Random();

  @Override
  public DroneInfo addNewDrone(int droneId, @Nonnull String ipAddress, int connectionPort)
      throws DroneIdAlreadyInUse {
    DroneInfo newDrone =
        new DroneInfo(droneId, CityPoint.randomPosition(random), 100, ipAddress, connectionPort);

    synchronized (drones) {
      if (drones.parallelStream().anyMatch(drone -> drone.getId() == droneId)) {
        throw new DroneIdAlreadyInUse();
      }
      drones.add(newDrone);
    }
    return newDrone;
  }

  @Override
  public synchronized void removeDroneById(int droneId) throws DroneIdNotFound {
    if (!drones.removeIf(drone -> drone.getId() == droneId)) {
      throw new DroneIdNotFound();
    }
  }

  @Override
  public synchronized Set<DroneInfo> getRegisteredDrones() {
    return new HashSet<>(drones);
  }
}
