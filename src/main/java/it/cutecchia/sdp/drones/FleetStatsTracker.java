package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.FleetStats;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class FleetStatsTracker extends Thread {
  private final Timer timer = new Timer();
  private final AdminServerClient client;

  private int deliveriesSinceLastUpdate = 0;
  private double kmsSinceLastUpdate = 0.0;
  private double pollutionSinceLastUpdate = 0.0;
  private final DroneStore store;

  public FleetStatsTracker(DroneStore drones, AdminServerClient client) {
    this.client = client;
    this.store = drones;
  }

  public void start() {
    Log.warn("FleetStatsTracker start");
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            FleetStats stats = calculateFleetStats();
            Log.warn("Sending stats to admin server %s", stats);
            client.sendFleetStats(stats);
            resetFleetStats();
          }
        },
        0,
        10 * 1000);
  }

  public void shutdown() {
    timer.cancel();
  }

  public void handleCompletedDeliveryStats(double pollution, double travelledKms) {
    deliveriesSinceLastUpdate++;
    kmsSinceLastUpdate += travelledKms;
    pollutionSinceLastUpdate += pollution;
  }

  private double calculateAverageBatteryLevel() {
    double averageBatteryLevel = 0.0;
    int dronesWithAvailableData = 0;
    for (DroneIdentifier id : store.getAllDroneIdentifiers()) {
      Optional<DroneData> data = store.getDroneData(id);
      if (!data.isPresent()) {
        continue;
      }
      averageBatteryLevel += data.get().getBatteryPercentage();
      dronesWithAvailableData++;
    }
    return averageBatteryLevel / dronesWithAvailableData;
  }

  private double calculateAverageCompletedDeliveries() {
    return (double) deliveriesSinceLastUpdate / store.getAllDroneIdentifiers().size();
  }

  private double calculateAveragePollutionLevel() {
    if (deliveriesSinceLastUpdate == 0) {
      return 0.0;
    }

    return pollutionSinceLastUpdate / deliveriesSinceLastUpdate;
  }

  private double calculateAverageTravelledKms() {
    return kmsSinceLastUpdate / store.getAllDroneIdentifiers().size();
  }

  private FleetStats calculateFleetStats() {
    return new FleetStats(
        System.currentTimeMillis(),
        calculateAverageCompletedDeliveries(),
        calculateAverageTravelledKms(),
        calculateAveragePollutionLevel(),
        calculateAverageBatteryLevel());
  }

  private void resetFleetStats() {
    deliveriesSinceLastUpdate = 0;
    kmsSinceLastUpdate = 0.0;
    pollutionSinceLastUpdate = 0.0;
  }
}
