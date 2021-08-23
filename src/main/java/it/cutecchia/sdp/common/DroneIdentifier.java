package it.cutecchia.sdp.common;

import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;
import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DroneIdentifier implements Comparable<DroneIdentifier> {
  private final int id;
  private final String ipAddress;
  private final int connectionPort;

  public DroneIdentifier(int id, @Nonnull String ipAddress, int connectionPort) {
    this.id = id;
    this.ipAddress = ipAddress;
    this.connectionPort = connectionPort;
  }

  public static DroneIdentifier fromProto(DroneServiceOuterClass.DroneIdentifierPacket proto) {
    return new DroneIdentifier(proto.getId(), proto.getAddress(), proto.getPort());
  }

  public DroneServiceOuterClass.DroneIdentifierPacket toProto() {
    return DroneServiceOuterClass.DroneIdentifierPacket.newBuilder()
        .setId(getId())
        .setAddress(getIpAddress())
        .setPort(getConnectionPort())
        .build();
  }

  public int getId() {
    return id;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public int getConnectionPort() {
    return connectionPort;
  }

  @Override
  public int compareTo(DroneIdentifier o) {
    return Integer.compare(getId(), o.getId());
  }

  @Override
  public String toString() {
    return String.format(
        "[Id: #%d, Address: %s, Port: %d]", getId(), getIpAddress(), getConnectionPort());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DroneIdentifier that = (DroneIdentifier) o;
    return id == that.id
        && connectionPort == that.connectionPort
        && ipAddress.equals(that.ipAddress);
  }

  @Override
  public int hashCode() {
    return id;
  }
}
