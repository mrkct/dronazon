package it.cutecchia.sdp.admin.server.beans;

import it.cutecchia.sdp.common.CityPoint;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneInfo {
  private final long id;
  private final CityPoint position;
  private final int batteryPercentage;
  private final boolean isMaster;

  public DroneInfo(long id, CityPoint position, int batteryPercentage, boolean isMaster) {
    this.id = id;
    this.position = position;
    this.batteryPercentage = batteryPercentage;
    this.isMaster = isMaster;
  }

  public long getId() {
    return id;
  }

  public CityPoint getPosition() {
    return position;
  }

  public DroneInfo moveTo(CityPoint newPosition) {
    return new DroneInfo(this.id, newPosition, this.batteryPercentage, this.isMaster);
  }

  public int getBatteryPercentage() {
    return batteryPercentage;
  }

  public boolean isMaster() {
    return isMaster;
  }
}
