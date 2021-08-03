package it.cutecchia.sdp.drones.messages;

import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;

public class CompletedDeliveryMessage {
  private final long timestamp;
  private final DroneIdentifier drone;
  private final Order order;
  private final double travelledKms;
  private final double pollution;
  private final int batteryPercentage;

  public CompletedDeliveryMessage(
      long timestamp,
      DroneIdentifier drone,
      Order order,
      double travelledKms,
      double pollution,
      int batteryPercentage) {
    this.timestamp = timestamp;
    this.drone = drone;
    this.order = order;
    this.travelledKms = travelledKms;
    this.pollution = pollution;
    this.batteryPercentage = batteryPercentage;
  }

  public static CompletedDeliveryMessage fromProto(
      DroneServiceOuterClass.CompletedDeliveryMessage proto) {
    return new CompletedDeliveryMessage(
        proto.getTimestamp(),
        DroneIdentifier.fromProto(proto.getSender()),
        Order.fromProto(proto.getCompletedOrder()),
        proto.getTravelledKms(),
        proto.getPollution(),
        proto.getBatteryPercentage());
  }

  public DroneServiceOuterClass.CompletedDeliveryMessage toProto() {
    return DroneServiceOuterClass.CompletedDeliveryMessage.newBuilder()
        .setTimestamp(timestamp)
        .setSender(drone.toProto())
        .setCompletedOrder(order.toProto())
        .setTravelledKms(travelledKms)
        .setPollution(pollution)
        .setBatteryPercentage(batteryPercentage)
        .build();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public DroneIdentifier getDrone() {
    return drone;
  }

  public Order getOrder() {
    return order;
  }

  public double getTravelledKms() {
    return travelledKms;
  }

  public double getPollution() {
    return pollution;
  }

  public int getBatteryPercentage() {
    return batteryPercentage;
  }
}
