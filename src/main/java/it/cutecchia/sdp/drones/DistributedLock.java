package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.ThreadUtils;
import it.cutecchia.sdp.drones.store.DroneStore;

public class DistributedLock {

  private static class LogicalClock {
    private int logicalClock = 0;

    public int value() {
      return logicalClock;
    }

    public synchronized int increment() {
      return ++logicalClock;
    }

    public synchronized void update(int otherLogicalClock) {
      logicalClock = Math.max(logicalClock, otherLogicalClock) + 1;
    }

    @Override
    public String toString() {
      return String.format("<Clock: %d>", logicalClock);
    }
  }

  private final DroneStore store;
  private final DroneCommunicationClient client;
  private final DroneIdentifier thisDrone;
  private final LogicalClock clock = new LogicalClock();

  private enum LockStatus {
    NOT_INTERESTED,
    WAITING,
    OWNED
  }

  private LockStatus lockStatus = LockStatus.NOT_INTERESTED;
  private int lockRequestTimestamp = -1;

  public DistributedLock(
      DroneIdentifier thisDrone, DroneStore store, DroneCommunicationClient client) {
    this.store = store;
    this.client = client;
    this.thisDrone = thisDrone;
  }

  public boolean isOwned() {
    return lockStatus == LockStatus.OWNED;
  }

  public synchronized void take() {
    lockStatus = LockStatus.WAITING;
    lockRequestTimestamp = clock.increment();

    ThreadUtils.spawnThreadForEach(
        store.getAllDroneIdentifiers(),
        drone -> {
          Log.debug("Requesting the lock from %s", drone);
          client.requestLock(drone, lockRequestTimestamp, thisDrone);
          Log.debug("%s gave up the lock!", drone);
        });

    lockStatus = LockStatus.OWNED;
  }

  public synchronized void release() {
    lockStatus = LockStatus.NOT_INTERESTED;
    notifyAll();
  }

  private synchronized void waitUntilDoneWithLock() {
    Log.debug("I will wait until im done with this lock");
    while (lockStatus != LockStatus.NOT_INTERESTED) {
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Log.debug("I am done with this lock");
  }

  public void onLockRequest(int logicalClock, DroneIdentifier requester) {
    Log.debug("Received a lock request <Clock=%d, Drone=%s>", logicalClock, requester);
    Log.debug(
        "Me: %d   Clock=%d    LockStatus=%s    lockRequestTimestamp=%d",
        thisDrone.getId(), clock.value(), lockStatus, lockRequestTimestamp);
    clock.update(logicalClock);
    if (lockStatus == LockStatus.NOT_INTERESTED) {
      Log.debug("I am not interested in the lock so I don't block");
    } else if (lockStatus == LockStatus.OWNED
        || (lockStatus == LockStatus.WAITING && lockRequestTimestamp < logicalClock)) {
      Log.debug("Added drone to the waiting list (my clock=%d)", lockRequestTimestamp);
      waitUntilDoneWithLock();
    } else {
      Log.debug(
          "Sent confirmation because they have precedence (my clock=%d)", lockRequestTimestamp);
    }
  }
}
