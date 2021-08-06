package it.cutecchia.sdp.drones;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import simulators.Measurement;

public class SlidingWindowTest {
  @Captor private ArgumentCaptor<List<Measurement>> captor;

  private final Measurement[] testMeasurements = {
    new Measurement("0", "pm10", 1.0, 0),
    new Measurement("1", "pm10", 2.0, 1),
    new Measurement("2", "pm10", 3.0, 2),
    new Measurement("3", "pm10", 4.0, 3),
    new Measurement("4", "pm10", 5.0, 4),
    new Measurement("5", "pm10", 6.0, 5),
    new Measurement("6", "pm10", 7.0, 6),
    new Measurement("7", "pm10", 8.0, 7),
  };

  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void initialWindow() {
    SlidingWindow.OnFullWindowAvailable callback = mock(SlidingWindow.OnFullWindowAvailable.class);
    SlidingWindow window = new SlidingWindow(4, 2, callback);

    for (int i = 0; i < 4; i++) {
      window.addMeasurement(testMeasurements[i]);
    }

    verify(callback, times(1)).onWindowAvailable(captor.capture());

    assertThat(captor.getValue())
        .asList()
        .containsExactly(
            testMeasurements[0], testMeasurements[1], testMeasurements[2], testMeasurements[3]);
  }

  @Test
  public void firstOverlappingWindow() {
    SlidingWindow.OnFullWindowAvailable callback = mock(SlidingWindow.OnFullWindowAvailable.class);
    SlidingWindow window = new SlidingWindow(4, 2, callback);

    for (int i = 0; i < 6; i++) {
      window.addMeasurement(testMeasurements[i]);
    }

    verify(callback, times(2)).onWindowAvailable(captor.capture());

    assertThat(captor.getAllValues().get(0)).asList().hasSize(4);
    assertThat(captor.getAllValues().get(1))
        .asList()
        .containsExactly(
            testMeasurements[2], testMeasurements[3], testMeasurements[4], testMeasurements[5]);
  }

  @Test
  public void secondOverlappingWindow() {
    SlidingWindow.OnFullWindowAvailable callback = mock(SlidingWindow.OnFullWindowAvailable.class);
    SlidingWindow window = new SlidingWindow(4, 2, callback);

    for (int i = 0; i < 8; i++) {
      window.addMeasurement(testMeasurements[i]);
    }

    verify(callback, times(3)).onWindowAvailable(captor.capture());

    assertThat(captor.getAllValues().get(0)).asList().hasSize(4);
    assertThat(captor.getAllValues().get(1)).asList().hasSize(4);
    assertThat(captor.getAllValues().get(2))
        .asList()
        .containsExactly(
            testMeasurements[4], testMeasurements[5], testMeasurements[6], testMeasurements[7]);
  }
}
