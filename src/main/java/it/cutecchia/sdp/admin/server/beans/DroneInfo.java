package it.cutecchia.sdp.admin.server.beans;

import it.cutecchia.sdp.common.CityPoint;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneInfo {
  private final long id;
  private final CityPoint position;
  private final int batteryPercentage;

  public DroneInfo(long id, CityPoint position, int batteryPercentage) {
    this.id = id;
    this.position = position;
    this.batteryPercentage = batteryPercentage;
  }

  public long getId() {
    return id;
  }

  public CityPoint getPosition() {
    return position;
  }

  public DroneInfo moveTo(CityPoint newPosition) {
    return new DroneInfo(this.id, newPosition, this.batteryPercentage);
  }

  public int getBatteryPercentage() {
    return batteryPercentage;
  }
}
