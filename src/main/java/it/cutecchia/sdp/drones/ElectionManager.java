package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.util.function.Function;

public class ElectionManager {
  private final DroneCommunicationClient client;
  private final Drone drone;
  private final DroneStore store;

  private volatile boolean isParticipant = false;

  public ElectionManager(Drone drone, DroneStore store, DroneCommunicationClient client) {
    this.drone = drone;
    this.store = store;
    this.client = client;
  }

  public void beginElection() {
    Log.notice("I am %d and I'm starting a new election", drone.getIdentifier().getId());
    isParticipant = true;
    trySendingToNextDroneInRing(
        (nextDrone) -> {
          if (drone.getIdentifier().equals(nextDrone)) {
            completeElection(nextDrone);
            return true;
          }
          return client.forwardElectionMessage(
              nextDrone, drone.getIdentifier(), drone.getData().getBatteryPercentage());
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
    boolean succeeded;
    do {
      DroneIdentifier nextDrone = store.getNextDroneInElectionRing(drone.getIdentifier());
      Log.info("trySendingToNextDroneInRing -> %s", nextDrone);

      succeeded = send.apply(nextDrone);
      if (!succeeded) store.signalFailedCommunicationWithDrone(nextDrone);
    } while (!succeeded);
  }

  private void completeElection(DroneIdentifier newMaster) {
    isParticipant = false;
    store.setKnownMaster(newMaster);
    if (this.drone.getIdentifier().equals(newMaster)) drone.becomeMaster();
    if (onNewMasterElectedListener != null) onNewMasterElectedListener.onNewMasterElected();
  }

  private boolean compareCandidates(
      DroneIdentifier left, int leftBattery, DroneIdentifier right, int rightBattery) {
    return leftBattery < rightBattery
        || (leftBattery == rightBattery && left.getId() < right.getId());
  }

  public void onElectionMessage(
      DroneIdentifier candidateMaster, int candidateMasterBatteryPercentage) {
    Log.info(
        "Received ELECTION with <P=%d - battery=%d%%>  Myself P=%d - battery=%d%%",
        candidateMaster.getId(),
        candidateMasterBatteryPercentage,
        drone.getIdentifier().getId(),
        drone.getData().getBatteryPercentage());

    final DroneIdentifier thisDrone = drone.getIdentifier();
    if (thisDrone.equals(candidateMaster)) {
      Log.info("I'm becoming the master. Forwarding ELECTED");
      isParticipant = false;
      trySendingToNextDroneInRing(
          (nextDrone) -> client.forwardElectedMessage(nextDrone, thisDrone));
      completeElection(thisDrone);
      return;
    }

    final int thisBatteryLevel = drone.getData().getBatteryPercentage();

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

  public void onElectedMessage(DroneIdentifier newMaster) {
    Log.notice("Received an ELECTED message: newMaster=%s", newMaster);
    final DroneIdentifier thisDrone = drone.getIdentifier();
    if (thisDrone.equals(newMaster)) {
      Log.info("Stopped forwarding the ELECTED message");
      return;
    }

    Log.info("Forwarding the ELECTED message to the next drone and completing the election");

    trySendingToNextDroneInRing(
        (nextDrone) -> {
          if (thisDrone.equals(nextDrone)) return true;
          return client.forwardElectedMessage(nextDrone, newMaster);
        });
    completeElection(newMaster);
  }

  public interface OnNewMasterElectedListener {
    void onNewMasterElected();
  }

  private OnNewMasterElectedListener onNewMasterElectedListener;

  public void setOnNewMasterElectedListener(OnNewMasterElectedListener listener) {
    onNewMasterElectedListener = listener;
  }

  public void clearOnNewMasterElectedListener() {
    onNewMasterElectedListener = null;
  }
}
