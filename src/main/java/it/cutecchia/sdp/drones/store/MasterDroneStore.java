package it.cutecchia.sdp.drones.store;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MasterDroneStore implements DroneStore {
  // FIXME: Instead of DroneIdentifier use an int, otherwise if another drone comes with different
  // address/port re-using the same id we might have consistency problems
  private final Map<DroneIdentifier, DroneData> drones = new HashMap<>();

  public MasterDroneStore() {}

  public MasterDroneStore(DroneStore store) {
    for (DroneIdentifier drone : store.getAllDroneIdentifiers()) {
      addDrone(drone);
    }
  }

  @Override
  public Set<DroneIdentifier> getAllDroneIdentifiers() {
    return drones.keySet();
  }

  @Override
  public void addDrone(DroneIdentifier identifier) {
    drones.put(identifier, null);
  }

  @Override
  public void removeDrone(DroneIdentifier identifier) {
    drones.remove(identifier);
  }

  @Override
  public Optional<DroneIdentifier> getNextDroneInElectionRing(DroneIdentifier identifier) {
    if (drones.keySet().size() == 0) {
      return Optional.empty();
    } else if (drones.keySet().size() == 1) {
      return drones.keySet().stream().findFirst();
    }

    Optional<DroneIdentifier> firstNextIdentifier =
        drones.keySet().parallelStream()
            .filter(id -> id.getId() > identifier.getId())
            .min(DroneIdentifier::compareTo);
    if (firstNextIdentifier.isPresent()) {
      return firstNextIdentifier;
    }

    return drones.keySet().parallelStream().min(DroneIdentifier::compareTo);
  }

  @Override
  public void handleDroneUpdateData(DroneIdentifier identifier, DroneData data) {
    if (!drones.containsKey(identifier)) {
      Log.warn(
          "Inserting data for drone #%d but drone was not recorded before%n", identifier.getId());
    }
    drones.put(identifier, data);
  }

  @Override
  public Optional<DroneData> getDroneData(DroneIdentifier identifier) {
    return Optional.ofNullable(drones.get(identifier));
  }

  @Override
  public Optional<DroneIdentifier> getAvailableDroneForOrder(Order order) {
    throw new IllegalArgumentException("not implemented");
    // return Optional.empty();
  }
}
