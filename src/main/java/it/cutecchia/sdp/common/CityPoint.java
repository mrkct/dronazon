package it.cutecchia.sdp.common;

import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;
import java.util.Random;

public class CityPoint {
  private static final Random random = new Random();

  public static final int CITY_WIDTH = 10;
  public static final int CITY_HEIGHT = 10;

  public final int x, y;

  public CityPoint(int x, int y) {
    if (x < 0 || y < 0 || x >= CITY_WIDTH || y >= CITY_HEIGHT) {
      throw new IllegalArgumentException("The coordinates are out of bounds");
    }
    this.x = x;
    this.y = y;
  }

  public static CityPoint fromProto(DroneServiceOuterClass.CityPointPacket proto) {
    return new CityPoint(proto.getX(), proto.getY());
  }

  public DroneServiceOuterClass.CityPointPacket toProto() {
    return DroneServiceOuterClass.CityPointPacket.newBuilder().setX(x).setY(y).build();
  }

  public static CityPoint randomPosition() {
    return new CityPoint(
        random.nextInt(CityPoint.CITY_WIDTH), random.nextInt(CityPoint.CITY_HEIGHT));
  }

  public double distanceTo(CityPoint other) {
    return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof CityPoint)) {
      return false;
    }
    CityPoint other = (CityPoint) obj;
    return x == other.x && y == other.y;
  }

  @Override
  public String toString() {
    return String.format("(%d, %d)", x, y);
  }
}
