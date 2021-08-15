package it.cutecchia.sdp.drones.states;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DataRaceTester;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.drones.Drone;
import it.cutecchia.sdp.drones.DroneCommunicationClient;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class RingSlaveState implements DroneState {
  private final Drone drone;
  private final DroneStore store;
  private final DroneCommunicationClient droneClient;
  private final AdminServerClient adminClient;
  private boolean shutdownInitiated = false;

  public RingSlaveState(
      Drone drone,
      DroneStore store,
      DroneCommunicationClient droneClient,
      AdminServerClient adminClient) {
    this.drone = drone;
    this.store = store;
    this.droneClient = droneClient;
    this.adminClient = adminClient;
  }

  @Override
  public void start() {
    CityPoint startingPosition = drone.getLocalData().getPosition();

    Set<DroneIdentifier> drones = new TreeSet<>(store.getAllDroneIdentifiers());
    drones.parallelStream()
        .forEach(
            (destination) -> {
              if (destination.equals(drone.getIdentifier())) {
                store.addDrone(destination);
                return;
              }

              Optional<DroneJoinResponse> response =
                  droneClient.notifyDroneJoin(destination, startingPosition);
              if (!response.isPresent()) {
                store.signalFailedCommunicationWithDrone(destination);
                return;
              }

              Log.info(
                  "notifyDroneJoin from #%d -> #%d success. %s",
                  drone.getIdentifier().getId(), destination.getId(), response.get());
              store.addDrone(destination);
              if (response.get().isMaster()) {
                store.setKnownMaster(destination);
              }
            });
  }

  @Override
  public void teardown() {}

  @Override
  public void initiateShutdown() {
    Log.notice("Initiated shutdown procedure (slave)");
    shutdownInitiated = true;
    synchronized (drone) {
      if (!drone.isDeliveringOrder()) {
        Log.notice("Drone is not currently delivering so I'm shutting down");
        forceShutdown();
      }
    }
  }

  private void forceShutdown() {
    Log.notice("Force Shutdown");
    droneClient.shutdown();
    adminClient.requestDroneExit(drone.getIdentifier());
    System.exit(0);
  }

  @Override
  public void afterCompletingAnOrder() {
    if (drone.getLocalData().isLowBattery()) initiateShutdown();

    if (shutdownInitiated) {
      DataRaceTester.sleep();
      forceShutdown();
    }
  }
}
