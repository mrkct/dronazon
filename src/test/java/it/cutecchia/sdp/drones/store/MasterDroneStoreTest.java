package it.cutecchia.sdp.drones.store;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import org.junit.jupiter.api.Test;

public class MasterDroneStoreTest {
  @Test
  public void simpleGetNextIdentifier() {
    DroneIdentifier a = new DroneIdentifier(1, "localhost", 0);
    DroneIdentifier b = new DroneIdentifier(2, "localhost", 0);
    DroneIdentifier c = new DroneIdentifier(3, "localhost", 0);

    MasterDroneStore store = new MasterDroneStore();
    store.addDrone(a);
    store.addDrone(b);
    store.addDrone(c);

    assertThat(store.getNextDroneInElectionRing(b)).contains(c);
  }

  @Test
  public void wrappingNextIdentifier() {
    DroneIdentifier a = new DroneIdentifier(1, "localhost", 0);
    DroneIdentifier b = new DroneIdentifier(2, "localhost", 0);
    DroneIdentifier c = new DroneIdentifier(3, "localhost", 0);

    MasterDroneStore store = new MasterDroneStore();
    store.addDrone(a);
    store.addDrone(b);
    store.addDrone(c);

    assertThat(store.getNextDroneInElectionRing(c)).contains(a);
  }

  @Test
  public void onlyOneDrone() {
    DroneIdentifier a = new DroneIdentifier(1, "localhost", 0);

    MasterDroneStore store = new MasterDroneStore();
    store.addDrone(a);

    assertThat(store.getNextDroneInElectionRing(a)).contains(a);
  }

  @Test
  public void noDrones() {
    DroneIdentifier a = new DroneIdentifier(1, "localhost", 0);

    MasterDroneStore store = new MasterDroneStore();

    assertThat(store.getNextDroneInElectionRing(a)).isEmpty();
  }

  @Test
  public void simpleDataStorage() {
    DroneIdentifier a = new DroneIdentifier(1, "localhost", 0);
    DroneData data = new DroneData(new CityPoint(2, 5), 75);

    MasterDroneStore store = new MasterDroneStore();

    store.addDrone(a);
    store.handleDroneUpdateData(a, data);

    assertThat(store.getDroneData(a)).contains(data);

    DroneData updatedData = new DroneData(new CityPoint(7, 4), 60);
    store.handleDroneUpdateData(a, updatedData);

    assertThat(store.getDroneData(a)).contains(updatedData);
  }
}
