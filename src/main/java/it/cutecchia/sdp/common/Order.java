package it.cutecchia.sdp.common;

public class Order {
  private final long id;
  private final CityPoint startPoint;
  private final CityPoint deliveryPoint;

  public Order(long id, CityPoint startPoint, CityPoint deliveryPoint) {
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

  public long getId() {
    return id;
  }

  @Override
  public String toString() {
    return String.format("[Order %d %s -> %s]", getId(), getStartPoint(), getDeliveryPoint());
  }
}
