package it.cutecchia.sdp.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneData {
  private final CityPoint position;
  private final int batteryPercentage;

  public DroneData(CityPoint position, int batteryPercentage) {
    this.position = position;
    this.batteryPercentage = batteryPercentage;
  }

  public DroneData(CityPoint position) {
    this(position, 100);
  }

  public CityPoint getPosition() {
    return position;
  }

  public DroneData moveTo(CityPoint newPosition) {
    return new DroneData(newPosition, this.batteryPercentage);
  }

  public int getBatteryPercentage() {
    return batteryPercentage;
  }
}
