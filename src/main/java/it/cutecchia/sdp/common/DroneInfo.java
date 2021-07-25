package it.cutecchia.sdp.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneInfo {
  private final CityPoint position;
  private final int batteryPercentage;

  public DroneInfo(CityPoint position, int batteryPercentage) {
    this.position = position;
    this.batteryPercentage = batteryPercentage;
  }

  public DroneInfo(CityPoint position) {
    this(position, 100);
  }

  public CityPoint getPosition() {
    return position;
  }

  public DroneInfo moveTo(CityPoint newPosition) {
    return new DroneInfo(newPosition, this.batteryPercentage);
  }

  public int getBatteryPercentage() {
    return batteryPercentage;
  }
}
