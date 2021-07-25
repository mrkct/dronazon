package it.cutecchia.sdp.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneIdentifier {
  private final int id;
  private final String ipAddress;
  private final int connectionPort;

  public DroneIdentifier(int id, String ipAddress, int connectionPort) {
    this.id = id;
    this.ipAddress = ipAddress;
    this.connectionPort = connectionPort;
  }

  public int getId() {
    return id;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public int getConnectionPort() {
    return connectionPort;
  }
}
