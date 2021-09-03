package it.cutecchia.sdp.common;

public class DataRaceTester {
  private DataRaceTester() {}

  private static final boolean SLEEP_ENABLED = false;
  private static final long NEXT_MILLIS_MULTIPLE = 400;

  public static void sleepForCollisions() {
    if (!SLEEP_ENABLED) return;
    try {
      long waitTime = NEXT_MILLIS_MULTIPLE - (System.currentTimeMillis() % NEXT_MILLIS_MULTIPLE);
      Log.info("DataRaceTester: Waiting for %dms...", waitTime);
      Thread.sleep(waitTime);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void sleep(long ms) {
    if (!SLEEP_ENABLED) return;
    try {
      Log.debug("---> %d: Starting to sleep for %d ms...", System.currentTimeMillis(), ms);
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
