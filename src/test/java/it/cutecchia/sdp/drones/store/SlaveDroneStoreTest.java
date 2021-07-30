package it.cutecchia.sdp.drones.store;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import it.cutecchia.sdp.common.DroneIdentifier;
import org.junit.jupiter.api.Test;

public class SlaveDroneStoreTest {
  @Test
  public void simpleGetNextIdentifier() {
    DroneIdentifier a = new DroneIdentifier(1, "localhost", 0);
    DroneIdentifier b = new DroneIdentifier(2, "localhost", 0);
    DroneIdentifier c = new DroneIdentifier(3, "localhost", 0);

    DroneStore store = new SlaveDroneStore();
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

    DroneStore store = new SlaveDroneStore();
    store.addDrone(a);
    store.addDrone(b);
    store.addDrone(c);

    assertThat(store.getNextDroneInElectionRing(c)).contains(a);
  }

  @Test
  public void onlyOneDrone() {
    DroneIdentifier a = new DroneIdentifier(1, "localhost", 0);

    DroneStore store = new SlaveDroneStore();
    store.addDrone(a);

    assertThat(store.getNextDroneInElectionRing(a)).contains(a);
  }

  @Test
  public void noDrones() {
    DroneIdentifier a = new DroneIdentifier(1, "localhost", 0);

    DroneStore store = new SlaveDroneStore();

    assertThat(store.getNextDroneInElectionRing(a)).isEmpty();
  }
}
