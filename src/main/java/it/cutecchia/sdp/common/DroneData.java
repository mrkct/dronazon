package it.cutecchia.sdp.common;

import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;
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

  public static DroneData fromProto(DroneServiceOuterClass.DroneDataPacket proto) {
    return new DroneData(
        CityPoint.fromProto(proto.getPosition()),
        proto.getBatteryPercentage(),
        proto.hasAssignedOrder() ? Order.fromProto(proto.getAssignedOrder()) : null);
  }

  public DroneServiceOuterClass.DroneDataPacket toProto() {
    DroneServiceOuterClass.DroneDataPacket.Builder builder =
        DroneServiceOuterClass.DroneDataPacket.newBuilder()
            .setPosition(position.toProto())
            .setBatteryPercentage(batteryPercentage);
    if (assignedOrder != null) {
      builder.setAssignedOrder(assignedOrder.toProto());
    } else {
      builder.clearAssignedOrder();
    }
    return builder.build();
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
    assert this.assignedOrder == null;
    return new DroneData(position, batteryPercentage, order);
  }

  public DroneData withoutOrder() {
    assert this.assignedOrder != null;
    return new DroneData(position, batteryPercentage, null);
  }

  public int getBatteryPercentage() {
    return batteryPercentage;
  }

  public DroneData decrementBattery(int howMuch) {
    return new DroneData(position, batteryPercentage - howMuch, assignedOrder);
  }

  public boolean isLowBattery() {
    return batteryPercentage < 15;
  }

  public boolean isAvailableForDeliveries() {
    return !isLowBattery() && assignedOrder == null;
  }

  @Override
  public String toString() {
    return String.format(
        "<Position=%s, Battery=%d%%, Order=%s>", position, batteryPercentage, assignedOrder);
  }
}
