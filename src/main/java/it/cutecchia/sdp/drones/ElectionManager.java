package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.DataRaceTester;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.ThreadUtils;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ElectionManager {
  private final DroneCommunicationClient client;
  private final Drone drone;
  private final DroneStore store;

  private volatile boolean isParticipant = false;
  private volatile boolean isElectionRunning = false;
  private volatile int electedMessagesToStop = 0;

  public ElectionManager(Drone drone, DroneStore store, DroneCommunicationClient client) {
    this.drone = drone;
    this.store = store;
    this.client = client;
  }

  public synchronized void waitUntilNoElectionIsHappening() {
    while (isElectionRunning) {
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assert electedMessagesToStop == 0;
  }

  public synchronized void beginElection() {
    Log.notice("I am %d and I'm starting a new election", drone.getIdentifier().getId());
    isParticipant = true;
    isElectionRunning = true;

    assert !drone.isMaster();

    trySendingToNextDroneInRing(
        (nextDrone) -> {
          final DroneIdentifier thisDrone = drone.getIdentifier();
          if (thisDrone.equals(nextDrone)) {
            Log.info("Skipping the election because I'm the only drone in the system");
            isElectionRunning = false;
            isParticipant = false;
            store.setKnownMaster(thisDrone);
            drone.becomeMaster();
            synchronized (this) {
              notifyAll();
            }
            callOnNewMasterElectedListeners();
            return true;
          }
          return client.forwardElectionMessage(
              nextDrone, drone.getIdentifier(), drone.getLocalData().getBatteryPercentage());
        });
  }

  /**
   * Get the next drone in the election ring and use the provided function to attempt sending a
   * message to that drone. The boolean return value of the provided function is used as an
   * indicator for whether the sending succeeded or failed. If it failed the drone store is notified
   * that drone is unreachable and the sending will be retried with the next drone in the election
   * ring until one succeeds
   *
   * @param send A function that takes the next drone in the election ring and is asked to send
   *     whatever message you want to that drone and return true if the message was delivered, false
   *     if the drone was unreachable
   */
  private void trySendingToNextDroneInRing(Function<DroneIdentifier, Boolean> send) {
    ThreadUtils.runInAnotherThread(
        () -> {
          boolean succeeded;
          do {
            DroneIdentifier nextDrone = store.getNextDroneInElectionRing(drone.getIdentifier());
            Log.info("trySendingToNextDroneInRing -> %s", nextDrone);

            succeeded = send.apply(nextDrone);
            if (!succeeded) store.signalFailedCommunicationWithDrone(nextDrone);
          } while (!succeeded);
        });
  }

  private boolean compareCandidates(
      DroneIdentifier left, int leftBattery, DroneIdentifier right, int rightBattery) {
    return leftBattery < rightBattery
        || (leftBattery == rightBattery && left.getId() < right.getId());
  }

  public synchronized void onElectionMessage(
      DroneIdentifier candidateMaster, int candidateMasterBatteryPercentage) {
    final DroneIdentifier thisDrone = drone.getIdentifier();
    isElectionRunning = true;

    // Edge case: A newly entered drone starts an election while another is running
    // and the new master is waiting to receive back its own ELECTION message
    // It will probably receive the original ELECTED message, but there is an case where it missed
    // it so we send another
    if (drone.isMaster()) {
      Log.warn("Received a ELECTION message when I am already master!");
      electedMessagesToStop++;
      trySendingToNextDroneInRing(
          (nextDrone) -> client.forwardElectedMessage(nextDrone, thisDrone));
      return;
    }

    Log.info(
        "Received ELECTION with <P=%d - battery=%d%%>  Myself P=%d - battery=%d%%",
        candidateMaster.getId(),
        candidateMasterBatteryPercentage,
        drone.getIdentifier().getId(),
        drone.getLocalData().getBatteryPercentage());

    DataRaceTester.sleep(3 * 1000);

    if (thisDrone.equals(candidateMaster)) {
      Log.info("I'm becoming the master. Forwarding ELECTED");
      isParticipant = false;
      store.setKnownMaster(thisDrone);
      drone.becomeMaster();

      electedMessagesToStop++;
      trySendingToNextDroneInRing(
          (nextDrone) -> client.forwardElectedMessage(nextDrone, thisDrone));
      return;
    }

    final int thisBatteryLevel = drone.getLocalData().getBatteryPercentage();

    if (compareCandidates(
        thisDrone, thisBatteryLevel, candidateMaster, candidateMasterBatteryPercentage)) {
      Log.info("Forwarding ELECTION untouched");
      isParticipant = true;
      trySendingToNextDroneInRing(
          (nextDrone) ->
              client.forwardElectionMessage(
                  nextDrone, candidateMaster, candidateMasterBatteryPercentage));
    } else if (!isParticipant
        && compareCandidates(
            candidateMaster, candidateMasterBatteryPercentage, thisDrone, thisBatteryLevel)) {
      Log.info("Forwarding ELECTION with myself as the new candidate");
      isParticipant = true;
      trySendingToNextDroneInRing(
          (nextDrone) -> client.forwardElectionMessage(nextDrone, thisDrone, thisBatteryLevel));
    } else {
      Log.info("Not forwarding the ELECTION message because it's useless");
    }
  }

  public synchronized void onElectedMessage(DroneIdentifier newMaster) {
    Log.info("Received an ELECTED message: newMaster=%s", newMaster);
    final DroneIdentifier thisDrone = drone.getIdentifier();

    if (thisDrone.equals(newMaster)) {
      Log.info("Stopped forwarding an ELECTED message");
      electedMessagesToStop--;
      if (electedMessagesToStop == 0) {
        isElectionRunning = false;
        notifyAll();
        callOnNewMasterElectedListeners();
      }
      return;
    }

    Log.info("Forwarding the ELECTED message to the next drone and completing the election");
    trySendingToNextDroneInRing(
        (nextDrone) -> {
          if (thisDrone.equals(nextDrone)) return true;
          return client.forwardElectedMessage(nextDrone, newMaster);
        });

    // Edge case: a drone enters in the midst of an election and tries to start a new once
    // It sends a ELECTION message, the new master puts itself and everyone receives
    // another ELECTED message
    if (newMaster.equals(store.getKnownMaster())) {
      return;
    }

    isParticipant = false;
    isElectionRunning = false;
    store.setKnownMaster(newMaster);
    notifyAll();
    callOnNewMasterElectedListeners();
  }

  public interface OnNewMasterElectedListener {
    void onNewMasterElected(OnNewMasterElectedListener thisListener);
  }

  private final List<OnNewMasterElectedListener> onNewMasterElectedListeners = new ArrayList<>();

  public void addOnNewMasterElectedListener(OnNewMasterElectedListener listener) {
    synchronized (onNewMasterElectedListeners) {
      onNewMasterElectedListeners.add(listener);
    }
  }

  public void clearOnNewMasterElectedListener(OnNewMasterElectedListener listener) {
    synchronized (onNewMasterElectedListeners) {
      onNewMasterElectedListeners.remove(listener);
    }
  }

  private void callOnNewMasterElectedListeners() {
    List<OnNewMasterElectedListener> listeners;
    synchronized (onNewMasterElectedListeners) {
      listeners = new ArrayList<>(onNewMasterElectedListeners);
    }

    Log.info(
        "Calling onNewMasterElectedListeners: %d listeners currently",
        onNewMasterElectedListeners.size());
    for (OnNewMasterElectedListener listener : listeners) {
      listener.onNewMasterElected(listener);
    }
  }
}
