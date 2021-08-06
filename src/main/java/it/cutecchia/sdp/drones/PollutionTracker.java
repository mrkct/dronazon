package it.cutecchia.sdp.drones;

import java.util.ArrayList;
import java.util.List;
import simulators.Buffer;
import simulators.Measurement;
import simulators.PM10Simulator;

public class PollutionTracker implements Buffer, SlidingWindow.OnFullWindowAvailable {
  private final SlidingWindow slidingWindow = new SlidingWindow(8, 4, this);
  private final PM10Simulator simulator = new PM10Simulator(this);
  private final List<Double> averageValues = new ArrayList<>();

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
    averageValues.add(average);
  }

  @Override
  public void addMeasurement(Measurement m) {
    slidingWindow.addMeasurement(m);
  }

  @Override
  public List<Measurement> readAllAndClean() {
    // FIXME: Don't know what to do with this
    return null;
  }

  public void clearAllMeasurements() {
    slidingWindow.clear();
    averageValues.clear();
  }

  public double getAverageMeasurementsValue() {
    double average = 0.0;
    for (double x : averageValues) {
      average += x;
    }
    return average / averageValues.size();
  }
}
