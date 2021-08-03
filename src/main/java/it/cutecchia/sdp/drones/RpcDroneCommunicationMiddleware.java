package it.cutecchia.sdp.drones;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import it.cutecchia.sdp.drones.grpc.DroneServiceGrpc;
import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
import it.cutecchia.sdp.drones.store.DroneStore;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RpcDroneCommunicationMiddleware extends DroneServiceGrpc.DroneServiceImplBase
    implements DroneCommunicationClient {
  private final Server rpcServer;
  private final DroneCommunicationServer droneServer;
  private final Map<DroneIdentifier, ManagedChannel> openChannels = new HashMap<>();

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

  private ManagedChannel getManagedChannel(DroneIdentifier drone) {
    Log.info("Connecting to %s...", drone);
    if (openChannels.containsKey(drone)) {
      Log.info("Reusing an already open channel");
      return openChannels.get(drone);
    }

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(drone.getIpAddress(), drone.getConnectionPort())
            .usePlaintext()
            .build();
    openChannels.put(drone, channel);
    return channel;
  }

  private DroneServiceGrpc.DroneServiceBlockingStub getBlockingStub(DroneIdentifier drone) {
    ManagedChannel channel = getManagedChannel(drone);
    return DroneServiceGrpc.newBlockingStub(channel).withDeadlineAfter(3, TimeUnit.SECONDS);
  }

  private DroneServiceGrpc.DroneServiceStub getAsyncStub(DroneIdentifier drone) {
    ManagedChannel channel = getManagedChannel(drone);
    return DroneServiceGrpc.newStub(channel).withDeadlineAfter(3, TimeUnit.SECONDS);
  }

  // The methods below this point are what a drone should call to do the operation

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
          getBlockingStub(destination).notifyDroneJoin(message);
      return Optional.of(DroneJoinResponse.fromProto(protoResponse));
    } catch (StatusRuntimeException e) {
      Log.error(
          "RPC Failed: notifyDroneJoin from #%d to #%d. Stack trace follows",
          sender.getId(), destination.getId());
      e.printStackTrace();
    }

    return Optional.empty();
  }

  @Override
  public void assignOrder(Order order, DroneIdentifier drone, AssignOrderCallback callback) {
    DroneServiceOuterClass.AssignOrderMessage message =
        DroneServiceOuterClass.AssignOrderMessage.newBuilder().setOrder(order.toProto()).build();
    getAsyncStub(drone)
        .assignOrder(
            message,
            new StreamObserver<DroneServiceOuterClass.Empty>() {
              @Override
              public void onNext(DroneServiceOuterClass.Empty value) {
                callback.onOrderAccepted();
              }

              @Override
              public void onError(Throwable t) {
                Log.error("Failed to assign order due to: %s", t.getMessage());
                t.printStackTrace();
                callback.onFailure();
              }

              @Override
              public void onCompleted() {}
            });
  }

  @Override
  public boolean notifyCompletedDelivery(
      DroneIdentifier masterDrone, CompletedDeliveryMessage message) {
    ManagedChannel channel = getManagedChannel(masterDrone);
    DroneServiceGrpc.DroneServiceBlockingStub stub =
        DroneServiceGrpc.newBlockingStub(channel).withDeadlineAfter(3, TimeUnit.SECONDS);

    try {
      stub.notifyCompletedDelivery(message.toProto());
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public void deliverToMaster(DroneStore store, DeliverToMasterCallback callback) {
    DroneIdentifier master = store.getKnownMaster();
    assert (master != null);
    boolean succeeded = callback.trySending(master);
    assert (succeeded);
    callback.onSuccess();
  }

  // The methods below this point are the gRpc service callbacks, you should NOT call these
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

  @Override
  public void assignOrder(
      DroneServiceOuterClass.AssignOrderMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    Order order = Order.fromProto(request.getOrder());
    droneServer.onOrderAssigned(order);

    responseObserver.onNext(DroneServiceOuterClass.Empty.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void notifyCompletedDelivery(
      DroneServiceOuterClass.CompletedDeliveryMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    droneServer.onCompletedDeliveryNotification(CompletedDeliveryMessage.fromProto(request));

    responseObserver.onNext(DroneServiceOuterClass.Empty.newBuilder().build());
    responseObserver.onCompleted();
  }
}
