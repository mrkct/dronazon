package it.cutecchia.sdp.drones.store;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import java.util.*;

public class SlaveDroneStore implements DroneStore {
  private final SortedSet<DroneIdentifier> drones = new TreeSet<>();

  public SlaveDroneStore() {}

  public SlaveDroneStore(DroneStore store) {
    store.getAllDroneIdentifiers().forEach(this::addDrone);
  }

  @Override
  public Set<DroneIdentifier> getAllDroneIdentifiers() {
    return drones;
  }

  @Override
  public void addDrone(DroneIdentifier identifier) {
    drones.add(identifier);
  }

  @Override
  public void removeDrone(DroneIdentifier identifier) {
    drones.remove(identifier);
  }

  @Override
  public Optional<DroneIdentifier> getNextDroneInElectionRing(DroneIdentifier identifier) {
    SortedSet<DroneIdentifier> tailSet = drones.tailSet(identifier);
    if (tailSet.size() == 0) {
      return Optional.empty();
    } else if (tailSet.size() == 1) {
      return Optional.of(drones.first());
    }

    return tailSet.stream().skip(1).findFirst();
  }

  @Override
  public void handleDroneUpdateData(DroneIdentifier identifier, DroneData data) {
    Log.warn("A slave drone was asked to handle data for drone #%d%n", identifier.getId());
  }

  @Override
  public Optional<DroneData> getDroneData(DroneIdentifier identifier) {
    Log.warn(String.format("A slave drone was asked data for drone #%d%n", identifier.getId()));
    return Optional.empty();
  }

  @Override
  public Optional<DroneIdentifier> getAvailableDroneForOrder(Order order) {
    Log.warn("A slave drone was asked to get an available drone for order #%d%n", order.getId());
    return Optional.empty();
  }
}
