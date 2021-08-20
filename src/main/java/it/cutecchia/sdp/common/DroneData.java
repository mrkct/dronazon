package it.cutecchia.sdp.common;

import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneData {
  private final CityPoint position;
  private final int batteryPercentage;
  private final Order assignedOrder;
  private final boolean isRecharging;

  public DroneData(
      CityPoint position, int batteryPercentage, Order assignedOrder, boolean isRecharging) {
    this.position = position;
    this.batteryPercentage = batteryPercentage;
    this.assignedOrder = assignedOrder;
    this.isRecharging = isRecharging;
  }

  public DroneData(CityPoint position, int batteryPercentage, Order order) {
    this(position, batteryPercentage, order, false);
  }

  public DroneData(CityPoint position, int batteryPercentage) {
    this(position, batteryPercentage, null, false);
  }

  public DroneData(CityPoint position) {
    this(position, 100);
  }

  public static DroneData fromProto(DroneServiceOuterClass.DroneDataPacket proto) {
    return new DroneData(
        CityPoint.fromProto(proto.getPosition()),
        proto.getBatteryPercentage(),
        proto.hasAssignedOrder() ? Order.fromProto(proto.getAssignedOrder()) : null,
        proto.getIsRecharging());
  }

  public DroneServiceOuterClass.DroneDataPacket toProto() {
    DroneServiceOuterClass.DroneDataPacket.Builder builder =
        DroneServiceOuterClass.DroneDataPacket.newBuilder()
            .setPosition(position.toProto())
            .setBatteryPercentage(batteryPercentage)
            .setIsRecharging(isRecharging);
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
    return !isLowBattery() && assignedOrder == null && !isRecharging;
  }

  public boolean isRecharging() {
    return isRecharging;
  }

  public DroneData startRecharging() {
    return new DroneData(position, batteryPercentage, assignedOrder, true);
  }

  @Override
  public String toString() {
    return String.format(
        "<Position=%s, Battery=%d%%, Order=%s, recharge=%b>",
        position, batteryPercentage, assignedOrder, isRecharging);
  }
}
