package it.cutecchia.sdp.admin.server.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Statistics {
    private final double averageDeliveries;
    private final double averageKmTravelled;
    private final double averagePollution;
    private final double averageBatteryLevel;

    public Statistics(double averageDeliveries, double averageKmTravelled, double averagePollution, double averageBatteryLevel) {
        this.averageDeliveries = averageDeliveries;
        this.averageKmTravelled = averageKmTravelled;
        this.averagePollution = averagePollution;
        this.averageBatteryLevel = averageBatteryLevel;
    }

    private static Statistics mostRecentStatistics = null;

    public static Statistics getMostRecentStatistics() { return mostRecentStatistics; }
    public static void updateStatistics(Statistics updated) { mostRecentStatistics = updated; }
}
