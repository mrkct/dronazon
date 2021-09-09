package it.cutecchia.sdp.drones;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import it.cutecchia.sdp.common.*;
import it.cutecchia.sdp.drones.store.InMemoryDroneStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OrderAssignerTest {
  @BeforeEach
  public void clearSpawnedThreadsList() {
    ThreadUtils.clearSpawnedThreadsList();
  }

  private static void waitForAllThreadsToBeDone() {
    for (Thread t : ThreadUtils.getAllSpawnedThreads()) {
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test
  public void closestDroneIsChosen()
      throws InterruptedException, DroneCommunicationClient.DroneIsUnreachable {
    DroneIdentifier closestDroneId = new DroneIdentifier(0, "123", 123);
    DroneIdentifier otherDroneId = new DroneIdentifier(1, "123", 123);
    DroneIdentifier otherDroneId2 = new DroneIdentifier(2, "123", 123);
    DroneIdentifier otherDroneId3 = new DroneIdentifier(3, "123", 123);

    InMemoryDroneStore store = new InMemoryDroneStore();
    store.addDrone(closestDroneId);
    store.handleDroneUpdateData(closestDroneId, new DroneData(new CityPoint(2, 3)));
    store.addDrone(otherDroneId);
    store.handleDroneUpdateData(otherDroneId, new DroneData(new CityPoint(0, 0)));
    store.addDrone(otherDroneId2);
    store.handleDroneUpdateData(otherDroneId2, new DroneData(new CityPoint(8, 8)));
    store.addDrone(otherDroneId3);
    store.handleDroneUpdateData(otherDroneId3, new DroneData(new CityPoint(9, 0)));

    Order order = new Order(0, new CityPoint(3, 3), new CityPoint(9, 9));

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);

    OrderAssigner assigner = new OrderAssigner(store, client);
    assigner.enqueueOrder(order);

    waitForAllThreadsToBeDone();

    verify(client, times(1)).assignOrder(same(order), same(closestDroneId));
  }

  @Test
  public void highestBatteryDroneIsChosenWhenTheyHaveTheSameDistanceToTheOrder()
      throws InterruptedException, DroneCommunicationClient.DroneIsUnreachable {
    DroneIdentifier highestBatteryDrone = new DroneIdentifier(0, "123", 123);
    DroneIdentifier otherDroneId = new DroneIdentifier(1, "123", 123);
    DroneIdentifier otherDroneId2 = new DroneIdentifier(2, "123", 123);
    DroneIdentifier otherDroneId3 = new DroneIdentifier(3, "123", 123);

    InMemoryDroneStore store = new InMemoryDroneStore();
    store.addDrone(highestBatteryDrone);
    store.handleDroneUpdateData(highestBatteryDrone, new DroneData(new CityPoint(2, 2), 80));
    store.addDrone(otherDroneId);
    store.handleDroneUpdateData(otherDroneId, new DroneData(new CityPoint(4, 4), 70));
    store.addDrone(otherDroneId2);
    store.handleDroneUpdateData(otherDroneId2, new DroneData(new CityPoint(4, 2), 60));
    store.addDrone(otherDroneId3);
    store.handleDroneUpdateData(otherDroneId3, new DroneData(new CityPoint(9, 0), 100));

    Order order = new Order(0, new CityPoint(3, 3), new CityPoint(9, 9));

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);

    OrderAssigner assigner = new OrderAssigner(store, client);
    assigner.enqueueOrder(order);

    waitForAllThreadsToBeDone();

    verify(client, times(1)).assignOrder(same(order), same(highestBatteryDrone));
  }

  @Test
  public void highestIdDroneIsChosenWhenTheyHaveTheSameDistanceAndSameBatteryLevel()
      throws InterruptedException, DroneCommunicationClient.DroneIsUnreachable {
    DroneIdentifier highestIdDrone = new DroneIdentifier(4, "123", 123);
    DroneIdentifier otherDroneId = new DroneIdentifier(3, "123", 123);
    DroneIdentifier otherDroneId2 = new DroneIdentifier(2, "123", 123);
    DroneIdentifier otherDroneId3 = new DroneIdentifier(1, "123", 123);

    InMemoryDroneStore store = new InMemoryDroneStore();
    store.addDrone(highestIdDrone);
    store.handleDroneUpdateData(highestIdDrone, new DroneData(new CityPoint(2, 2), 80));
    store.addDrone(otherDroneId);
    store.handleDroneUpdateData(otherDroneId, new DroneData(new CityPoint(4, 4), 80));
    store.addDrone(otherDroneId2);
    store.handleDroneUpdateData(otherDroneId2, new DroneData(new CityPoint(4, 2), 80));
    store.addDrone(otherDroneId3);
    store.handleDroneUpdateData(otherDroneId3, new DroneData(new CityPoint(9, 0), 100));

    Order order = new Order(0, new CityPoint(3, 3), new CityPoint(9, 9));

    DroneCommunicationClient client = mock(DroneCommunicationClient.class);

    OrderAssigner assigner = new OrderAssigner(store, client);
    assigner.enqueueOrder(order);

    waitForAllThreadsToBeDone();

    verify(client, times(1)).assignOrder(same(order), same(highestIdDrone));
  }
}
