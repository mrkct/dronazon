package it.cutecchia.sdp.admin.server.messages;

import it.cutecchia.sdp.common.DroneIdentifier;
import javax.xml.bind.annotation.XmlRootElement;

/** This object is used to represent a drone's request to enter the system. */
@XmlRootElement
public class DroneEnterRequest {
  public DroneEnterRequest() {}

  public DroneEnterRequest(DroneIdentifier drone) {
    this.ipAddress = drone.getIpAddress();
    this.connectionPort = drone.getConnectionPort();
  }

  private String ipAddress;
  private int connectionPort;

  public String getIpAddress() {
    return ipAddress;
  }

  public int getConnectionPort() {
    return connectionPort;
  }
}
