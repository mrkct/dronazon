package it.cutecchia.sdp.website;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.Order;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class PeriodicOrderGenerator extends OrderReceiver {
  private int nextId = 0;

  private final Timer timer;
  private final long millisBetweenEachOrder;

  public PeriodicOrderGenerator(long millisBetweenEachOrder, Timer timer, Random randomGenerator) {
    this.millisBetweenEachOrder = millisBetweenEachOrder;
    this.timer = timer;
  }

  public void begin() {
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            notifyListeners(generateOrder());
          }
        },
        0,
        millisBetweenEachOrder);
  }

  private Order generateOrder() {
    return new Order(nextId++, CityPoint.randomPosition(), CityPoint.randomPosition());
  }
}
