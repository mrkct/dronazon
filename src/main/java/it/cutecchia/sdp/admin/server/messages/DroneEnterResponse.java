package it.cutecchia.sdp.admin.server.messages;

import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneEnterResponse {
  private final CityPoint newlyAddedDronePosition;
  private final Set<DroneIdentifier> allDrones;

  public DroneEnterResponse(CityPoint newlyAddedDronePosition, Set<DroneIdentifier> allDrones) {
    this.newlyAddedDronePosition = newlyAddedDronePosition;
    this.allDrones = allDrones;
  }

  public Set<DroneIdentifier> getAllDrones() {
    return allDrones;
  }

  public CityPoint getNewlyAddedDronePosition() {
    return newlyAddedDronePosition;
  }
}
