package it.cutecchia.sdp.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FleetStats {
  private final long timestamp;
  private final double averageDeliveries;
  private final double averageKmTravelled;
  private final double averagePollution;
  private final double averageBatteryLevel;

  public FleetStats(
      long timestamp,
      double averageDeliveries,
      double averageKmTravelled,
      double averagePollution,
      double averageBatteryLevel) {
    this.timestamp = timestamp;
    this.averageDeliveries = averageDeliveries;
    this.averageKmTravelled = averageKmTravelled;
    this.averagePollution = averagePollution;
    this.averageBatteryLevel = averageBatteryLevel;
  }

  public double getAverageDeliveries() {
    return averageDeliveries;
  }

  public double getAverageKmTravelled() {
    return averageKmTravelled;
  }

  public double getAveragePollution() {
    return averagePollution;
  }

  public double getAverageBatteryLevel() {
    return averageBatteryLevel;
  }
}
