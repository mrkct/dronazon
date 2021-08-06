package it.cutecchia.sdp.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FleetStats implements Comparable<FleetStats> {
  private long timestamp;
  private double averageDeliveries;
  private double averageKmTravelled;
  private double averagePollution;
  private double averageBatteryLevel;

  public FleetStats() {}

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

  public long getTimestamp() {
    return timestamp;
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

  @Override
  public String toString() {
    return String.format(
        "[Ts: %d, Deliveries: %f, Kms: %f, Pollution: %f, Battery: %f]",
        timestamp, averageDeliveries, averageKmTravelled, averagePollution, averageBatteryLevel);
  }

  @Override
  public int compareTo(FleetStats o) {
    return Long.compare(timestamp, o.timestamp);
  }
}
