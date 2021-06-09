package it.cutecchia.sdp.admin.server.responses;

import it.cutecchia.sdp.admin.server.beans.DroneInfo;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneEnterResponse {
  private final DroneInfo newlyAddedDroneInfo;
  private final Set<DroneInfo> allDrones;
  private final DroneInfo masterDrone;

  public DroneEnterResponse(
      DroneInfo newlyAddedDroneInfo, Set<DroneInfo> allDrones, DroneInfo masterDrone) {
    this.newlyAddedDroneInfo = newlyAddedDroneInfo;
    this.allDrones = allDrones;
    this.masterDrone = masterDrone;
  }

  public Set<DroneInfo> getAllDrones() {
    return allDrones;
  }

  public DroneInfo getNewlyAddedDroneInfo() {
    return newlyAddedDroneInfo;
  }

  public DroneInfo getMasterDrone() {
    return masterDrone;
  }
}
