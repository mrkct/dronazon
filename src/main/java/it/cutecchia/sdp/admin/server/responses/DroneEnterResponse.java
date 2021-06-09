package it.cutecchia.sdp.admin.server.responses;

import it.cutecchia.sdp.admin.server.beans.DroneInfo;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneEnterResponse {
  private final DroneInfo newlyAddedDroneInfo;
  private final Set<DroneInfo> allDrones;

  public DroneEnterResponse(DroneInfo newlyAddedDroneInfo, Set<DroneInfo> allDrones) {
    this.newlyAddedDroneInfo = newlyAddedDroneInfo;
    this.allDrones = allDrones;
  }

  public Set<DroneInfo> getAllDrones() {
    return allDrones;
  }

  public DroneInfo getNewlyAddedDroneInfo() {
    return newlyAddedDroneInfo;
  }
}
