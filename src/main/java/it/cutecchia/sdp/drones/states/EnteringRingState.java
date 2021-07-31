package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.drones.Drone;
import it.cutecchia.sdp.drones.DroneCommunicationClient;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.Optional;

public class EnteringRingState implements DroneState {
  private final Drone drone;
  private final DroneStore store;
  private final DroneCommunicationClient client;

  public EnteringRingState(Drone drone, DroneStore store, DroneCommunicationClient client) {
    this.drone = drone;
    this.store = store;
    this.client = client;
  }

  @Override
  public void start() {
    CityPoint startingPosition = drone.getData().getPosition();
    store.getAllDroneIdentifiers().parallelStream()
        .forEach(
            (destination) -> {
              if (destination.equals(drone.getIdentifier())) {
                return;
              }

              Optional<DroneJoinResponse> response =
                  client.notifyDroneJoin(destination, drone.getIdentifier(), startingPosition);
              if (!response.isPresent()) {
                return;
              }

              Log.info(
                  "notifyDroneJoin from #%d -> #%d success. %s",
                  drone.getIdentifier().getId(), destination.getId(), response.get().toString());
            });
  }

  @Override
  public void teardown() {}

  @Override
  public void shutdown() {}
}
