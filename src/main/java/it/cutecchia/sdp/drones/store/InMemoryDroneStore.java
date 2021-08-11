package it.cutecchia.sdp.drones.store;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import java.util.*;
import javax.annotation.Nonnull;

public class InMemoryDroneStore implements DroneStore {
  // FIXME: Instead of DroneIdentifier use an int, otherwise if another drone comes with different
  // address/port re-using the same id we might have consistency problems
  private final Map<DroneIdentifier, DroneData> drones = new HashMap<>();
  private DroneIdentifier knownMaster = null;

  public InMemoryDroneStore() {}

  @Override
  public Set<DroneIdentifier> getAllDroneIdentifiers() {
    return drones.keySet();
  }

  @Override
  public synchronized void addDrone(DroneIdentifier identifier) {
    drones.put(identifier, null);
  }

  @Override
  public DroneIdentifier getNextDroneInElectionRing(DroneIdentifier identifier) {
    assert drones.keySet().size() > 0;

    if (drones.keySet().size() == 1) {
      return drones.keySet().stream().findFirst().get();
    }

    Optional<DroneIdentifier> firstNextIdentifier =
        drones.keySet().parallelStream()
            .filter(id -> id.getId() > identifier.getId())
            .min(DroneIdentifier::compareTo);
    if (firstNextIdentifier.isPresent()) {
      return firstNextIdentifier.get();
    }

    return drones.keySet().parallelStream().min(DroneIdentifier::compareTo).get();
  }

  @Override
  public synchronized void handleDroneUpdateData(DroneIdentifier identifier, DroneData data) {
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
  public synchronized void signalFailedCommunicationWithDrone(DroneIdentifier drone) {
    Log.info("Master store was signalled that drone %s is not reachable.", drone);
    drones.remove(drone);
    if (drone.equals(knownMaster)) {
      knownMaster = null;
    }
  }

  @Override
  public void signalDroneWasAssignedOrder(@Nonnull DroneIdentifier drone, @Nonnull Order order) {
    assert drones.containsKey(drone);
    assert getDroneData(drone).isPresent();
    assert getDroneData(drone).get().getAssignedOrder() == null;

    handleDroneUpdateData(drone, getDroneData(drone).get().withOrder(order));
  }

  @Override
  public void setKnownMaster(DroneIdentifier drone) {
    assert drones.containsKey(drone);
    knownMaster = drone;
  }

  @Override
  public DroneIdentifier getKnownMaster() {
    return knownMaster;
  }
}
