package it.cutecchia.sdp.drones;

import static org.mockito.Mockito.*;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.store.InMemoryDroneStore;
import org.junit.jupiter.api.Test;

public class OrderAssignerTest {
  @Test
  public void closestDroneIsChosen() {
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

    verify(client, times(1)).assignOrder(same(order), same(closestDroneId), any());
  }

  @Test
  public void highestBatteryDroneIsChosenWhenTheyHaveTheSameDistanceToTheOrder() {
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

    verify(client, times(1)).assignOrder(same(order), same(highestBatteryDrone), any());
  }

  @Test
  public void highestIdDroneIsChosenWhenTheyHaveTheSameDistanceAndSameBatteryLevel() {
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

    verify(client, times(1)).assignOrder(same(order), same(highestIdDrone), any());
  }
}
