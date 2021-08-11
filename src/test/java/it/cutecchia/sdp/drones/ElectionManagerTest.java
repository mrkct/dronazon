package it.cutecchia.sdp.drones;

import static org.mockito.Mockito.*;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.drones.store.DroneStore;
import org.junit.jupiter.api.Test;

public class ElectionManagerTest {
  @Test
  public void beginElectionSendsMessageToTheNextDrone() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);
    final int thisDroneBattery = 80;
    final DroneIdentifier nextDroneInRing = new DroneIdentifier(2, "0.0.0.0", 1);

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);
    when(drone.getData()).thenReturn(new DroneData(new CityPoint(0, 0), thisDroneBattery));

    DroneStore store = mock(DroneStore.class);
    when(store.getNextDroneInElectionRing(any())).thenReturn(nextDroneInRing);

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);
    when(client.forwardElectionMessage(any(), any(), anyInt())).thenReturn(true);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.beginElection();

    verify(client).forwardElectionMessage(nextDroneInRing, thisDrone, thisDroneBattery);
  }

  @Test
  public void electionIsSkippedWhenTheDroneIsTheLastOne() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);
    final int thisDroneBattery = 80;

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);
    when(drone.getData()).thenReturn(new DroneData(new CityPoint(0, 0), thisDroneBattery));

    DroneStore store = mock(DroneStore.class);
    when(store.getNextDroneInElectionRing(any())).thenReturn(thisDrone);

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);
    when(client.forwardElectionMessage(any(), any(), anyInt())).thenReturn(true);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.beginElection();

    verifyNoInteractions(client);
    verify(store).setKnownMaster(eq(thisDrone));
    verify(drone).becomeMaster();
  }

  @Test
  public void nonParticipantAndTheElectionMessageIsBetterCandidate() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);
    final int thisDroneBattery = 80;
    final DroneIdentifier candidate = new DroneIdentifier(2, "0.0.0.0", 1);
    final int candidateBattery = 100;

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);
    when(drone.getData()).thenReturn(new DroneData(new CityPoint(0, 0), thisDroneBattery));

    DroneStore store = mock(DroneStore.class);
    when(store.getNextDroneInElectionRing(any())).thenReturn(thisDrone);

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);
    when(client.forwardElectionMessage(any(), any(), anyInt())).thenReturn(true);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.onElectionMessage(candidate, candidateBattery);

    verify(client).forwardElectionMessage(any(), eq(candidate), eq(candidateBattery));
  }

  @Test
  public void nonParticipantAndTheElectionMessageIsWorseCandidate() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);
    final int thisDroneBattery = 80;
    final DroneIdentifier candidate = new DroneIdentifier(2, "0.0.0.0", 1);
    final int candidateBattery = 70;

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);
    when(drone.getData()).thenReturn(new DroneData(new CityPoint(0, 0), thisDroneBattery));

    DroneStore store = mock(DroneStore.class);
    when(store.getNextDroneInElectionRing(any())).thenReturn(thisDrone);

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);
    when(client.forwardElectionMessage(any(), any(), anyInt())).thenReturn(true);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.onElectionMessage(candidate, candidateBattery);

    verify(client).forwardElectionMessage(any(), eq(thisDrone), eq(thisDroneBattery));
  }

  @Test
  public void avoidForwardingUselessElectionMessage() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);
    final int thisDroneBattery = 80;
    final DroneIdentifier forwardedCandidate = new DroneIdentifier(2, "0.0.0.0", 1);
    final int forwardedCandidateBattery = 100;
    final DroneIdentifier blockedCandidate = new DroneIdentifier(3, "0.0.0.0", 2);
    final int blockedCandidateBattery = 90;

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);
    when(drone.getData()).thenReturn(new DroneData(new CityPoint(0, 0), thisDroneBattery));

    DroneStore store = mock(DroneStore.class);
    when(store.getNextDroneInElectionRing(any())).thenReturn(thisDrone);

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);
    when(client.forwardElectionMessage(any(), any(), anyInt())).thenReturn(true);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.onElectionMessage(forwardedCandidate, forwardedCandidateBattery);
    electionManager.onElectionMessage(blockedCandidate, blockedCandidateBattery);

    verify(client, times(1))
        .forwardElectionMessage(any(), eq(forwardedCandidate), eq(forwardedCandidateBattery));
  }

  @Test
  public void forwardMultipleElectionMessageIfItsBetterCandidate() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);
    final int thisDroneBattery = 80;
    final DroneIdentifier firstCandidate = new DroneIdentifier(2, "0.0.0.0", 1);
    final int firstCandidateBattery = 90;
    final DroneIdentifier secondCandidate = new DroneIdentifier(3, "0.0.0.0", 2);
    final int secondCandidateBattery = 100;

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);
    when(drone.getData()).thenReturn(new DroneData(new CityPoint(0, 0), thisDroneBattery));

    DroneStore store = mock(DroneStore.class);
    when(store.getNextDroneInElectionRing(any())).thenReturn(thisDrone);

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);
    when(client.forwardElectionMessage(any(), any(), anyInt())).thenReturn(true);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.onElectionMessage(firstCandidate, firstCandidateBattery);
    electionManager.onElectionMessage(secondCandidate, secondCandidateBattery);

    verify(client, times(2)).forwardElectionMessage(any(), any(), anyInt());
    verify(client, atLeastOnce())
        .forwardElectionMessage(any(), eq(firstCandidate), eq(firstCandidateBattery));
    verify(client, atLeastOnce())
        .forwardElectionMessage(any(), eq(secondCandidate), eq(secondCandidateBattery));
  }

  @Test
  public void receiveBackElectionMessage() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);
    final int thisDroneBattery = 80;
    final DroneIdentifier anotherDrone = new DroneIdentifier(2, "0.0.0.0", 1);

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);
    when(drone.getData()).thenReturn(new DroneData(new CityPoint(0, 0), thisDroneBattery));

    DroneStore store = mock(DroneStore.class);
    when(store.getNextDroneInElectionRing(any())).thenReturn(anotherDrone);

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);
    when(client.forwardElectedMessage(eq(anotherDrone), eq(thisDrone))).thenReturn(true);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.onElectionMessage(thisDrone, thisDroneBattery);

    verify(client).forwardElectedMessage(eq(anotherDrone), eq(thisDrone));
    verify(store).setKnownMaster(eq(thisDrone));
    verify(drone).becomeMaster();
  }

  @Test
  public void receiveAnotherDroneElectedMessage() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);
    final int thisDroneBattery = 80;
    final DroneIdentifier newMaster = new DroneIdentifier(2, "0.0.0.0", 1);
    final DroneIdentifier nextDroneInRing = new DroneIdentifier(3, "0.0.0.0", 2);

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);
    when(drone.getData()).thenReturn(new DroneData(new CityPoint(0, 0), thisDroneBattery));

    DroneStore store = mock(DroneStore.class);
    when(store.getNextDroneInElectionRing(any())).thenReturn(nextDroneInRing);

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);
    when(client.forwardElectedMessage(eq(nextDroneInRing), eq(newMaster))).thenReturn(true);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.onElectedMessage(newMaster);

    verify(client).forwardElectedMessage(eq(nextDroneInRing), eq(newMaster));
    verify(store).setKnownMaster(eq(newMaster));
    verify(drone, never()).becomeMaster();
  }

  @Test
  public void receiveBackMyElectedMessage() {
    final DroneIdentifier thisDrone = new DroneIdentifier(1, "0.0.0.0", 0);

    Drone drone = mock(Drone.class);
    when(drone.getIdentifier()).thenReturn(thisDrone);

    DroneStore store = mock(DroneStore.class);
    DroneCommunicationClient client = mock(DroneCommunicationClient.class);

    ElectionManager electionManager = new ElectionManager(drone, store, client);
    electionManager.onElectedMessage(thisDrone);

    verifyNoInteractions(client);
  }
}
