package it.cutecchia.sdp.common;

import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Order {
  private final int id;
  private final CityPoint startPoint;
  private final CityPoint deliveryPoint;

  public Order(int id, CityPoint startPoint, CityPoint deliveryPoint) {
    this.id = id;
    this.startPoint = startPoint;
    this.deliveryPoint = deliveryPoint;
  }

  public CityPoint getDeliveryPoint() {
    return deliveryPoint;
  }

  public CityPoint getStartPoint() {
    return startPoint;
  }

  public int getId() {
    return id;
  }

  public DroneServiceOuterClass.OrderPacket toProto() {
    return DroneServiceOuterClass.OrderPacket.newBuilder()
        .setId(getId())
        .setStartingPoint(startPoint.toProto())
        .setDeliveryPoint(deliveryPoint.toProto())
        .build();
  }

  public static Order fromProto(DroneServiceOuterClass.OrderPacket proto) {
    return new Order(
        proto.getId(),
        CityPoint.fromProto(proto.getStartingPoint()),
        CityPoint.fromProto(proto.getDeliveryPoint()));
  }

  @Override
  public String toString() {
    return String.format("[Order %d %s -> %s]", getId(), getStartPoint(), getDeliveryPoint());
  }
}
