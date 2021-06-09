package it.cutecchia.sdp.admin.server.beans;

import it.cutecchia.sdp.common.CityPoint;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneInfo {
  private final long id;
  private final CityPoint position;
  private final int batteryPercentage;

  private final String ipAddress;
  private final int connectionPort;

  public DroneInfo(long id, CityPoint position, int batteryPercentage, String ipAddress, int connectionPort) {
    this.id = id;
    this.position = position;
    this.batteryPercentage = batteryPercentage;
    this.ipAddress = ipAddress;
    this.connectionPort = connectionPort;
  }

  public long getId() {
    return id;
  }

  public CityPoint getPosition() {
    return position;
  }

  public DroneInfo moveTo(CityPoint newPosition) {
    return new DroneInfo(this.id, newPosition, this.batteryPercentage, this.ipAddress, this.connectionPort);
  }

  public int getBatteryPercentage() {
    return batteryPercentage;
  }

  public String getIpAddress() { return ipAddress; }

  public int getConnectionPort() { return connectionPort; }
}
