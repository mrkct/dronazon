package it.cutecchia.sdp.drones.store;

import it.cutecchia.sdp.common.*;
import java.util.*;
import javax.annotation.Nonnull;

public class InMemoryDroneStore implements DroneStore {
  // FIXME: Forse dovrebbero essere syncronized tanti di questi metodi perchè non vogliamo update
  // sotto al naso
  private final Map<DroneIdentifier, DroneData> drones = new HashMap<>();
  private DroneIdentifier knownMaster = null;

  public InMemoryDroneStore() {}

  @Override
  public Set<DroneIdentifier> getAllDroneIdentifiers() {
    synchronized (drones) {
      return new TreeSet<>(drones.keySet());
    }
  }

  @Override
  public void addDrone(DroneIdentifier identifier, DroneData data) {
    synchronized (drones) {
      drones.put(identifier, data);
    }
  }

  @Override
  public DroneIdentifier getNextDroneInElectionRing(DroneIdentifier identifier) {
    assert drones.keySet().size() > 0;

    Set<DroneIdentifier> drones = getAllDroneIdentifiers();
    if (drones.size() == 1) {
      return drones.stream().findFirst().get();
    }

    Optional<DroneIdentifier> firstNextIdentifier =
        drones
            .parallelStream()
            .filter(id -> id.getId() > identifier.getId())
            .min(DroneIdentifier::compareTo);
    if (firstNextIdentifier.isPresent()) {
      return firstNextIdentifier.get();
    }

    return drones.parallelStream().min(DroneIdentifier::compareTo).get();
  }

  @Override
  public void handleDroneUpdateData(DroneIdentifier identifier, DroneData data) {
    synchronized (drones) {
      if (!drones.containsKey(identifier)) {
        Log.warn(
            "Inserting data for drone #%d but drone was not recorded before%n", identifier.getId());
      }
      drones.put(identifier, data);
    }
  }

  @Override
  public Optional<DroneData> getDroneData(DroneIdentifier identifier) {
    return Optional.ofNullable(drones.get(identifier));
  }

  @Override
  public void signalFailedCommunicationWithDrone(DroneIdentifier drone) {
    Log.info("Master store was signalled that drone %s is not reachable.", drone);
    synchronized (drones) {
      drones.remove(drone);
      if (drone.equals(knownMaster)) {
        knownMaster = null;
      }
    }
  }

  @Override
  public void signalDroneWasAssignedOrder(@Nonnull DroneIdentifier drone, @Nonnull Order order) {
    assert getDroneData(drone).isPresent();
    assert getDroneData(drone).get().getAssignedOrder() == null;

    handleDroneUpdateData(drone, getDroneData(drone).get().withOrder(order));
  }

  @Override
  public void signalDroneIsRecharging(DroneIdentifier drone) {
    assert getDroneData(drone).isPresent();
    assert getDroneData(drone).get().canAcceptOrders();

    handleDroneUpdateData(drone, getDroneData(drone).get().refuseOrders());
  }

  @Override
  public void signalDroneCompletedCharging(DroneIdentifier drone) {
    DroneData data =
        new DroneData(
            new CityPoint(0, 0),
            100,
            getDroneData(drone).map(DroneData::getAssignedOrder).orElse(null),
            true);
    drones.put(drone, data);
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
