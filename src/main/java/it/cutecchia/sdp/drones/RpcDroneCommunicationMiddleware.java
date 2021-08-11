package it.cutecchia.sdp.drones;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import it.cutecchia.sdp.common.*;
import it.cutecchia.sdp.drones.grpc.DroneServiceGrpc;
import it.cutecchia.sdp.drones.grpc.DroneServiceOuterClass;
import it.cutecchia.sdp.drones.messages.CompletedDeliveryMessage;
import it.cutecchia.sdp.drones.responses.DroneJoinResponse;
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
  private final DroneIdentifier drone;

  /**
   * A middleware between a drone and other drones in the network based on gRpc.
   *
   * @param drone: The drone that will be considered as the sender/receiver of all requests
   * @param service: An object that will be forwarded each request that is made to this drone
   */
  public RpcDroneCommunicationMiddleware(DroneIdentifier drone, DroneCommunicationServer service) {
    this.drone = drone;
    this.rpcServer = ServerBuilder.forPort(drone.getConnectionPort()).addService(this).build();
    this.droneServer = service;
  }

  public void startRpcServer() throws IOException {
    rpcServer.start();
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

  // The methods below this point are what a drone should call to do the operation

  /**
   * Notify another drone that this drone (the one passed in the constructor) has just joined the
   * system. This is blocking
   *
   * @param destination The drone you want to notify
   * @param startingPosition The position where the new drone is currently at
   * @return The drone response if the drone was reachable, an empty optional if the drone couldn't
   *     be reached
   */
  @Override
  public Optional<DroneJoinResponse> notifyDroneJoin(
      DroneIdentifier destination, CityPoint startingPosition) {
    DroneServiceOuterClass.DroneJoinMessage message =
        DroneServiceOuterClass.DroneJoinMessage.newBuilder()
            .setSender(drone.toProto())
            .setStartingPosition(startingPosition.toProto())
            .build();
    try {
      DroneServiceOuterClass.DroneJoinResponse protoResponse =
          getBlockingStub(destination).notifyDroneJoin(message);
      return Optional.of(DroneJoinResponse.fromProto(protoResponse));
    } catch (StatusRuntimeException e) {
      Log.error(
          "RPC Failed: notifyDroneJoin from #%d to #%d. Stack trace follows",
          drone.getId(), destination.getId());
      e.printStackTrace();
    }

    return Optional.empty();
  }

  /**
   * Assign an order to a drone, requesting that it delivers that order. This method should only be
   * used by the master since it is the only one with the authority to assign orders. This is
   * blocking.
   *
   * @param order The order you want to assign
   * @param drone The drone you would like to deliver the order
   */
  @Override
  public boolean assignOrder(Order order, DroneIdentifier drone) {
    DroneServiceOuterClass.AssignOrderMessage message =
        DroneServiceOuterClass.AssignOrderMessage.newBuilder().setOrder(order.toProto()).build();
    try {
      return Context.current()
          .fork()
          .call(
              () -> {
                getBlockingStub(drone).assignOrder(message);
                return true;
              });
    } catch (Exception e) {
      Log.warn(
          "Failed to assign order with context %s due to: %s", Context.current(), e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Notify the master drone that this drone has completed delivering the previously assigned order.
   * This also sends updated data about this drone. This is blocking.
   *
   * @param masterDrone The master drone to whom the notification message will be sent
   * @param message The message that will be sent
   * @return true if the communication with the master drone succeeded, false if the master drone
   *     was not reachable
   */
  @Override
  public boolean notifyCompletedDelivery(
      DroneIdentifier masterDrone, CompletedDeliveryMessage message) {
    DroneServiceGrpc.DroneServiceBlockingStub stub = getBlockingStub(masterDrone);

    try {
      stub.notifyCompletedDelivery(message.toProto());
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(
          "Failed to notify completed delivery to %d due to: %s",
          masterDrone.getId(), e.getMessage());
      // e.printStackTrace();
      return false;
    }
  }

  /**
   * Request a drone's data. This is blocking.
   *
   * @param drone The drone to which request the data
   * @return The drone's data if the communication is successful, empty if the drone couldn't be
   *     reached
   */
  @Override
  public Optional<DroneData> requestData(DroneIdentifier drone) {
    try {
      return Optional.of(DroneData.fromProto(getBlockingStub(drone).requestData(empty())));
    } catch (StatusRuntimeException e) {
      Log.warn("Failed to request data from %s: %s", drone, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public boolean forwardElectionMessage(
      DroneIdentifier destination,
      DroneIdentifier candidateLeader,
      int candidateLeaderBatteryPercentage) {
    DroneServiceOuterClass.ElectionMessage message =
        DroneServiceOuterClass.ElectionMessage.newBuilder()
            .setCandidateLeader(candidateLeader.toProto())
            .setCandidateLeaderBatteryPercentage(candidateLeaderBatteryPercentage)
            .build();

    try {
      getBlockingStub(destination).notifyElectionMessage(message);
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(
          "Failed to send ELECTION message to %d due to: %s", destination.getId(), e.getMessage());
      return false;
    }
  }

  @Override
  public boolean forwardElectedMessage(DroneIdentifier destination, DroneIdentifier newLeader) {
    DroneServiceOuterClass.ElectedMessage message =
        DroneServiceOuterClass.ElectedMessage.newBuilder()
            .setNewLeader(newLeader.toProto())
            .build();

    try {
      getBlockingStub(destination).notifyElectedMessage(message);
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(
          "Failed to send ELECTED message to %d due to: %s", destination.getId(), e.getMessage());
      return false;
    }
  }

  @Override
  public synchronized void shutdown() {
    Log.notice("Shutting down the middleware...");
    for (ManagedChannel channel : openChannels.values()) {
      channel.shutdown();
    }
    openChannels.clear();
    rpcServer.shutdown();
  }

  // The methods below this point are the gRpc service callbacks, you should NOT call these
  @Override
  public void notifyDroneJoin(
      DroneServiceOuterClass.DroneJoinMessage request,
      StreamObserver<DroneServiceOuterClass.DroneJoinResponse> responseObserver) {

    DroneIdentifier sender = DroneIdentifier.fromProto(request.getSender());
    CityPoint startingPosition = CityPoint.fromProto(request.getStartingPosition());
    Log.notice("Received a request to join the ring from drone #%d", sender.getId());

    DroneJoinResponse response = droneServer.onDroneJoin(sender, startingPosition);

    responseObserver.onNext(response.toProto());
    responseObserver.onCompleted();
  }

  @Override
  public void assignOrder(
      DroneServiceOuterClass.AssignOrderMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    Order order = Order.fromProto(request.getOrder());

    Context.current().fork().run(() -> droneServer.onOrderAssigned(order));

    responseObserver.onNext(empty());
    responseObserver.onCompleted();
  }

  @Override
  public void notifyCompletedDelivery(
      DroneServiceOuterClass.CompletedDeliveryMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    responseObserver.onNext(empty());
    responseObserver.onCompleted();

    droneServer.onCompletedDeliveryNotification(CompletedDeliveryMessage.fromProto(request));
  }

  @Override
  public void requestData(
      DroneServiceOuterClass.Empty request,
      StreamObserver<DroneServiceOuterClass.DroneDataPacket> responseObserver) {
    DroneData data = droneServer.onDataRequest();
    responseObserver.onNext(data.toProto());
    responseObserver.onCompleted();
  }

  @Override
  public void notifyElectionMessage(
      DroneServiceOuterClass.ElectionMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    responseObserver.onNext(empty());
    responseObserver.onCompleted();

    Context.current()
        .fork()
        .run(
            () ->
                droneServer.onElectionMessage(
                    DroneIdentifier.fromProto(request.getCandidateLeader()),
                    request.getCandidateLeaderBatteryPercentage()));
  }

  @Override
  public void notifyElectedMessage(
      DroneServiceOuterClass.ElectedMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    responseObserver.onNext(empty());
    responseObserver.onCompleted();

    Context.current()
        .fork()
        .run(() -> droneServer.onElectedMessage(DroneIdentifier.fromProto(request.getNewLeader())));
  }

  private DroneServiceOuterClass.Empty empty() {
    return DroneServiceOuterClass.Empty.newBuilder().build();
  }
}
