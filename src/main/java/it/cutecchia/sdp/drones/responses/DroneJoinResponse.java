package it.cutecchia.sdp.drones.responses;

import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;

public class DroneJoinResponse {
  private final DroneIdentifier identifier;
  private final boolean isMaster;

  public DroneJoinResponse(DroneIdentifier identifier, boolean isMaster) {
    this.identifier = identifier;
    this.isMaster = isMaster;
  }

  public static DroneJoinResponse fromProto(DroneServiceOuterClass.DroneJoinResponse protoMessage) {
    return new DroneJoinResponse(
        DroneIdentifier.fromProto(protoMessage.getSender()), protoMessage.getIsMaster());
  }

  public DroneIdentifier getIdentifier() {
    return identifier;
  }

  public boolean isMaster() {
    return isMaster;
  }

  public DroneServiceOuterClass.DroneJoinResponse toProto() {
    return DroneServiceOuterClass.DroneJoinResponse.newBuilder()
        .setIsMaster(isMaster())
        .setSender(identifier.toProto())
        .build();
  }

  @Override
  public String toString() {
    return String.format(
        "<DroneJoinResponse: Identifier=%s, isMaster=%b>", identifier.toString(), isMaster());
  }
}
