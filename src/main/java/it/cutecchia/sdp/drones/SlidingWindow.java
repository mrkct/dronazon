package it.cutecchia.sdp.drones;

import java.util.ArrayList;
import java.util.List;
import simulators.Measurement;

public class SlidingWindow {
  public interface OnFullWindowAvailable {
    void onWindowAvailable(List<Measurement> measurements);
  }

  private final int windowSize, windowOverlap;
  private final OnFullWindowAvailable callback;

  private final List<Measurement> recentMeasurements = new ArrayList<>();

  public SlidingWindow(int windowSize, int windowOverlap, OnFullWindowAvailable callback) {
    assert (windowSize > windowOverlap);
    this.windowSize = windowSize;
    this.windowOverlap = windowOverlap;
    this.callback = callback;
  }

  public synchronized void addMeasurement(Measurement measurement) {
    recentMeasurements.add(measurement);

    int measurements = recentMeasurements.size();
    if (measurements < windowSize) {
      return;
    }

    if (measurements == windowSize) {
      callback.onWindowAvailable(recentMeasurements);
      return;
    }

    if (measurements == windowSize + windowOverlap) {
      for (int i = 0; i < windowOverlap; i++) {
        recentMeasurements.remove(0);
      }
      callback.onWindowAvailable(recentMeasurements);
    }
  }

  void clear() {
    recentMeasurements.clear();
  }
}
