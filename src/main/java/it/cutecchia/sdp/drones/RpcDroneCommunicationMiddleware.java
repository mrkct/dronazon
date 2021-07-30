package it.cutecchia.sdp.drones;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.drones.grpc.DroneServiceGrpc;
import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import java.io.IOException;
import java.util.Optional;

public class RpcDroneCommunicationMiddleware extends DroneServiceGrpc.DroneServiceImplBase
    implements DroneCommunicationClient {
  private final Server rpcServer;
  private final DroneCommunicationServer droneServer;

  public RpcDroneCommunicationMiddleware(DroneIdentifier drone, DroneCommunicationServer service) {
    this.rpcServer = ServerBuilder.forPort(drone.getConnectionPort()).addService(this).build();
    this.droneServer = service;
  }

  public void startRpcServer() throws IOException {
    rpcServer.start();
  }

  public void shutdownRpcServer() {
    rpcServer.shutdown();
  }

  private DroneServiceGrpc.DroneServiceBlockingStub getStub(DroneIdentifier identifier) {
    Log.info("Connecting to %s...", identifier.toString());
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(identifier.getIpAddress(), identifier.getConnectionPort())
            .usePlaintext()
            .build();
    return DroneServiceGrpc.newBlockingStub(channel);
  }

  @Override
  public Optional<DroneJoinResponse> notifyDroneJoin(
      DroneIdentifier destination, DroneIdentifier sender, CityPoint startingPosition) {
    DroneServiceOuterClass.DroneJoinMessage message =
        DroneServiceOuterClass.DroneJoinMessage.newBuilder()
            .setSender(sender.toProto())
            .setStartingPosition(startingPosition.toProto())
            .build();

    try {
      DroneServiceOuterClass.DroneJoinResponse protoResponse =
          getStub(destination).notifyDroneJoin(message);
      return Optional.of(DroneJoinResponse.fromProto(protoResponse));
    } catch (StatusRuntimeException e) {
      Log.error(
          "RPC Failed: notifyDroneJoin from #%d to #%d. Stack trace follows",
          sender.getId(), destination.getId());
      e.printStackTrace();
    }
    return Optional.empty();
  }

  // ^DroneServiceImplBase
  @Override
  public void notifyDroneJoin(
      DroneServiceOuterClass.DroneJoinMessage request,
      StreamObserver<DroneServiceOuterClass.DroneJoinResponse> responseObserver) {

    DroneIdentifier sender = DroneIdentifier.fromProto(request.getSender());
    CityPoint startingPosition = CityPoint.fromProto(request.getStartingPosition());
    Log.info("Received a request to join the ring from drone #%d", sender.getId());

    DroneJoinResponse response = droneServer.onDroneJoin(sender, startingPosition);

    responseObserver.onNext(response.toProto());
    responseObserver.onCompleted();
  }
}
