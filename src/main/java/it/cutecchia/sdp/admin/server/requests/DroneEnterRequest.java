package it.cutecchia.sdp.admin.server.requests;

import javax.xml.bind.annotation.XmlRootElement;

/** This object is used to represent a drone's request to enter the system. */
@XmlRootElement
public class DroneEnterRequest {
  private String ipAddress;
  private int connectionPort;

  public String getIpAddress() {
    return ipAddress;
  }

  public int getConnectionPort() {
    return connectionPort;
  }
}
