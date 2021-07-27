package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.DroneData;
import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.Set;

public interface VirtualDroneStore {
  class VirtualDrone {
    public final DroneIdentifier identifier;
    public final DroneData data;

    public VirtualDrone(DroneIdentifier identifier, DroneData data) {
      this.identifier = identifier;
      this.data = data;
    }
  }

  interface OnUpdateReceivedListener {
    void onBroadcastInfoRequestSuccess();

    void onBroadcastInfoRequestFailure(Set<DroneData> failedCommunications);
  }

  void broadcastUpdateInfoRequest(
      DroneCommunicationClient communicationClient, OnUpdateReceivedListener listener);

  Set<VirtualDrone> getAllAvailableDronesInfo();

  void updateDroneInfo(DroneData info);
}
