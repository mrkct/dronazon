package it.cutecchia.sdp.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneData {
  private final CityPoint position;
  private final int batteryPercentage;
  private final Order assignedOrder;

  public DroneData(CityPoint position, int batteryPercentage, Order assignedOrder) {
    this.position = position;
    this.batteryPercentage = batteryPercentage;
    this.assignedOrder = assignedOrder;
  }

  public DroneData(CityPoint position, int batteryPercentage) {
    this(position, batteryPercentage, null);
  }

  public DroneData(CityPoint position) {
    this(position, 100);
  }

  public CityPoint getPosition() {
    return position;
  }

  public Order getAssignedOrder() {
    return assignedOrder;
  }

  public DroneData moveTo(CityPoint newPosition) {
    return new DroneData(newPosition, this.batteryPercentage, this.assignedOrder);
  }

  public DroneData withOrder(Order order) {
    assert (this.assignedOrder == null);
    return new DroneData(position, batteryPercentage, order);
  }

  public DroneData withoutOrder() {
    assert (this.assignedOrder != null);
    return new DroneData(position, batteryPercentage, null);
  }

  public int getBatteryPercentage() {
    return batteryPercentage;
  }

  @Override
  public String toString() {
    return String.format(
        "<Position=%s, Battery=%d%%, Order=%s>", position, batteryPercentage, assignedOrder);
  }
}
