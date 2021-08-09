package it.cutecchia.sdp.admin.server.stores;

import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.*;
import javax.annotation.Nonnull;

public class InMemoryDronesStore implements DronesStore {
  private static final InMemoryDronesStore instance = new InMemoryDronesStore();

  public static InMemoryDronesStore getInstance() {
    return instance;
  }

  private final Set<DroneIdentifier> drones = new HashSet<>();

  @Override
  public void addNewDrone(int droneId, @Nonnull String ipAddress, int connectionPort)
      throws DroneIdAlreadyInUse {
    DroneIdentifier newDrone = new DroneIdentifier(droneId, ipAddress, connectionPort);

    synchronized (drones) {
      if (drones.parallelStream().anyMatch(drone -> drone.getId() == droneId)) {
        throw new DroneIdAlreadyInUse();
      }
      drones.add(newDrone);
    }
  }

  @Override
  public synchronized void removeDroneById(int droneId) throws DroneIdNotFound {
    if (!drones.removeIf(drone -> drone.getId() == droneId)) {
      throw new DroneIdNotFound();
    }
  }

  @Override
  public synchronized Set<DroneIdentifier> getRegisteredDrones() {
    return new HashSet<>(drones);
  }

  @Override
  public synchronized void clear() {
    drones.clear();
  }
}
