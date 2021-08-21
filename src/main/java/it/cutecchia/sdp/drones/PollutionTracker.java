package it.cutecchia.sdp.drones;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import simulators.Buffer;
import simulators.Measurement;
import simulators.PM10Simulator;

public class PollutionTracker implements Buffer, SlidingWindow.OnFullWindowAvailable {
  private final SlidingWindow slidingWindow = new SlidingWindow(8, 4, this);
  private final PM10Simulator simulator = new PM10Simulator(this);

  private final Object averageMeasurementsLock = new Object();
  private List<Measurement> averageMeasurements = new ArrayList<>();

  public void startTracking() {
    simulator.start();
  }

  public void stopTracking() {
    simulator.stopMeGently();
  }

  @Override
  public void onWindowAvailable(List<Measurement> measurements) {
    double average = 0.0;
    for (Measurement m : measurements) {
      average += m.getValue();
    }
    average /= measurements.size();

    Measurement measurement = new Measurement("0", "average", average, System.currentTimeMillis());
    synchronized (averageMeasurementsLock) {
      averageMeasurements.add(measurement);
    }
  }

  @Override
  public void addMeasurement(Measurement m) {
    slidingWindow.addMeasurement(m);
  }

  @Override
  public List<Measurement> readAllAndClean() {
    List<Measurement> toReturn = averageMeasurements;
    synchronized (averageMeasurementsLock) {
      averageMeasurements = new ArrayList<>();
    }

    return toReturn;
  }

  public List<Double> readAllAndCleanAsDoubles() {
    return readAllAndClean().stream().map(Measurement::getValue).collect(Collectors.toList());
  }
}
