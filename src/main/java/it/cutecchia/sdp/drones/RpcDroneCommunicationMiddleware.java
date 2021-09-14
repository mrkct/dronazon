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
    synchronized (openChannels) {
      if (openChannels.containsKey(drone)) {
        Log.info("Reusing an already open channel");
        return openChannels.get(drone);
      }
    }

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(drone.getIpAddress(), drone.getConnectionPort())
            .usePlaintext()
            .build();
    synchronized (openChannels) {
      openChannels.put(drone, channel);
    }

    return channel;
  }

  private void destroyChannel(DroneIdentifier drone) {
    Log.info("Destroying channel to drone %s", drone);
    ManagedChannel channel;
    synchronized (openChannels) {
      channel = openChannels.remove(drone);
    }
    if (channel != null && !channel.isShutdown()) {
      channel.shutdown();
    }
  }

  private DroneServiceGrpc.DroneServiceBlockingStub getBlockingStub(DroneIdentifier drone) {
    ManagedChannel channel = getManagedChannel(drone);
    return DroneServiceGrpc.newBlockingStub(channel).withDeadlineAfter(30, TimeUnit.SECONDS);
  }

  private DroneServiceGrpc.DroneServiceBlockingStub getBlockingStubWithoutDeadline(
      DroneIdentifier drone) {
    ManagedChannel channel = getManagedChannel(drone);
    return DroneServiceGrpc.newBlockingStub(channel);
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
      DataRaceTester.sleepForCollisions();
      DroneServiceOuterClass.DroneJoinResponse protoResponse =
          getBlockingStub(destination).notifyDroneJoin(message);
      return Optional.of(DroneJoinResponse.fromProto(protoResponse));
    } catch (StatusRuntimeException e) {
      Log.warn(
          "RPC Failed: notifyDroneJoin to #%d due to: %s", destination.getId(), e.getMessage());
      destroyChannel(destination);
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
   * @return <code>true</code> if the drone was reachable and accepted to deliver the order, <code>
   *     false</code> if the drone received the request but refused to deliver the order
   * @throws DroneIsUnreachable The drone is unreachable due to network issues
   */
  @Override
  public boolean assignOrder(Order order, DroneIdentifier drone) throws DroneIsUnreachable {
    DroneServiceOuterClass.AssignOrderMessage message =
        DroneServiceOuterClass.AssignOrderMessage.newBuilder().setOrder(order.toProto()).build();
    try {
      return Context.current()
          .fork()
          .call(
              () -> {
                DataRaceTester.sleepForCollisions();
                return getBlockingStub(drone).assignOrder(message).getAccepted();
              });
    } catch (Exception e) {
      Log.warn(
          "Failed to assign order with context %s due to: %s", Context.current(), e.getMessage());
      destroyChannel(drone);
      throw new DroneIsUnreachable();
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
      DataRaceTester.sleepForCollisions();
      stub.notifyCompletedDelivery(message.toProto());
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(
          "Failed to notify completed delivery to %d due to: %s",
          masterDrone.getId(), e.getMessage());
      destroyChannel(masterDrone);
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
      DataRaceTester.sleepForCollisions();
      return Optional.of(DroneData.fromProto(getBlockingStub(drone).requestData(empty())));
    } catch (StatusRuntimeException e) {
      Log.warn("Failed to request data from %s: %s", drone, e.getMessage());
      destroyChannel(drone);
      return Optional.empty();
    }
  }

  /**
   * Send an ELECTION message to <code>destination</code> containing the other parameters as the
   * current best candidate for the next master. This is blocking
   *
   * @param destination The drone that will receive the message
   * @param candidateLeader The current best candidate for the position of master
   * @param candidateLeaderBatteryPercentage The battery percentage of the candidate
   * @return <code>true</code>if <code>destination</code> received the message successfully, <code>
   *     false</code> if the communication failed somehow
   */
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
      DataRaceTester.sleepForCollisions();
      getBlockingStub(destination).notifyElectionMessage(message);
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(
          "Failed to send ELECTION message to %d due to: %s", destination.getId(), e.getMessage());
      destroyChannel(destination);
      return false;
    }
  }

  /**
   * Sends an ELECTED message to <code>destination</code> notifying that <code>newLeader</code> has
   * been elected as the new master drone of the system
   *
   * @param destination The drone that will receive the message
   * @param newLeader The drone that has been elected as the new leader of the system
   * @return <code>true</code>if <code>destination</code> received the message successfully, <code>
   *     false</code> if * the communication failed somehow
   */
  @Override
  public boolean forwardElectedMessage(DroneIdentifier destination, DroneIdentifier newLeader) {
    DroneServiceOuterClass.ElectedMessage message =
        DroneServiceOuterClass.ElectedMessage.newBuilder()
            .setNewLeader(newLeader.toProto())
            .build();
    try {
      DataRaceTester.sleepForCollisions();
      getBlockingStub(destination).notifyElectedMessage(message);
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(
          "Failed to send ELECTED message to %d due to: %s", destination.getId(), e.getMessage());
      destroyChannel(destination);
      return false;
    }
  }

  /**
   * Sends an empty message to a drone and requests that it responds. This is used to check that a
   * drone is alive and can respond to messages This is blocking
   *
   * @param destination The drone to which the message will be sent
   * @return <code>true</code> if the drone responded, <code>false</code> if it was unreachable
   */
  @Override
  public boolean requestHeartbeat(DroneIdentifier destination) {
    try {
      Log.info("Sending HEARTBEAT to %d", destination.getId());
      getBlockingStub(destination).requestHeartbeat(empty());
      Log.info("Received HEARTBEAT from %d", destination.getId());
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(
          "Failed to receive HEARTBEAT from %d due to: %s", destination.getId(), e.getMessage());
      destroyChannel(destination);
      return false;
    }
  }

  @Override
  public boolean notifyCompletedCharging(DroneIdentifier destination, DroneIdentifier sender) {
    try {
      Log.info("Sending COMPLETED_CHARGING to %d", destination.getId());

      getBlockingStub(destination).notifyCompletedCharging(sender.toProto());
      return true;
    } catch (StatusRuntimeException e) {
      Log.warn(
          "Failed to send COMPLETED_CHARGING to %d due to: %s",
          destination.getId(), e.getMessage());
      destroyChannel(destination);
      return false;
    }
  }

  @Override
  public void requestLock(
      DroneIdentifier destination, int logicalClock, DroneIdentifier requester) {
    try {
      DroneServiceOuterClass.LockRequestMessage message =
          DroneServiceOuterClass.LockRequestMessage.newBuilder()
              .setRequester(requester.toProto())
              .setLogicalClock(logicalClock)
              .build();
      getBlockingStubWithoutDeadline(destination).requestLock(message);
    } catch (StatusRuntimeException e) {
      Log.warn("Failed to send REQUEST_LOCK to %d due to: %s", destination.getId(), e.getMessage());
      destroyChannel(destination);
    }
  }

  @Override
  public synchronized void shutdown() {
    Log.notice("Shutting down the middleware...");
    ThreadUtils.spawnThreadForEach(
        openChannels.entrySet(),
        (entry) -> {
          final ManagedChannel channel = entry.getValue();
          final DroneIdentifier drone = entry.getKey();

          channel.shutdownNow();
          try {
            channel.awaitTermination(2, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Log.warn("Failed to shutdown channel to %s due to: %s", drone, e.getMessage());
          }
        });
    openChannels.clear();
    rpcServer.shutdownNow();
  }

  // The methods below this point are the gRpc service callbacks, you should NOT call these
  @Override
  public void notifyDroneJoin(
      DroneServiceOuterClass.DroneJoinMessage request,
      StreamObserver<DroneServiceOuterClass.DroneJoinResponse> responseObserver) {
    DataRaceTester.sleepForCollisions();

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
      StreamObserver<DroneServiceOuterClass.AssignOrderResponse> responseObserver) {
    DataRaceTester.sleepForCollisions();
    Order order = Order.fromProto(request.getOrder());

    boolean acceptOrder = droneServer.onOrderAssigned(order);
    DroneServiceOuterClass.AssignOrderResponse message =
        DroneServiceOuterClass.AssignOrderResponse.newBuilder().setAccepted(acceptOrder).build();

    responseObserver.onNext(message);
    responseObserver.onCompleted();
  }

  @Override
  public void notifyCompletedDelivery(
      DroneServiceOuterClass.CompletedDeliveryMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    DataRaceTester.sleepForCollisions();
    responseObserver.onNext(empty());
    responseObserver.onCompleted();

    DataRaceTester.sleepForCollisions();
    droneServer.onCompletedDeliveryNotification(CompletedDeliveryMessage.fromProto(request));
  }

  @Override
  public void requestData(
      DroneServiceOuterClass.Empty request,
      StreamObserver<DroneServiceOuterClass.DroneDataPacket> responseObserver) {
    DataRaceTester.sleepForCollisions();
    DroneData data = droneServer.onDataRequest();
    responseObserver.onNext(data.toProto());
    responseObserver.onCompleted();
  }

  @Override
  public void notifyElectionMessage(
      DroneServiceOuterClass.ElectionMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    Context.current()
        .fork()
        .run(
            () -> {
              DataRaceTester.sleepForCollisions();
              droneServer.onElectionMessage(
                  DroneIdentifier.fromProto(request.getCandidateLeader()),
                  request.getCandidateLeaderBatteryPercentage());
            });

    responseObserver.onNext(empty());
    responseObserver.onCompleted();
  }

  @Override
  public void notifyElectedMessage(
      DroneServiceOuterClass.ElectedMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {

    Context.current()
        .fork()
        .run(
            () -> {
              DataRaceTester.sleepForCollisions();
              droneServer.onElectedMessage(DroneIdentifier.fromProto(request.getNewLeader()));
            });

    responseObserver.onNext(empty());
    responseObserver.onCompleted();
  }

  @Override
  public void requestHeartbeat(
      DroneServiceOuterClass.Empty request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    responseObserver.onNext(empty());
    responseObserver.onCompleted();
  }

  @Override
  public void requestLock(
      DroneServiceOuterClass.LockRequestMessage request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    // onLockRequest will block until the lock is available. We have removed the deadline from this
    // request

    droneServer.onLockRequest(
        request.getLogicalClock(), DroneIdentifier.fromProto(request.getRequester()));

    responseObserver.onNext(empty());
    responseObserver.onCompleted();
  }

  @Override
  public void notifyCompletedCharging(
      DroneServiceOuterClass.DroneIdentifierPacket request,
      StreamObserver<DroneServiceOuterClass.Empty> responseObserver) {
    responseObserver.onNext(empty());
    responseObserver.onCompleted();

    DataRaceTester.sleepForCollisions();
    droneServer.onCompletedChargeMessage(DroneIdentifier.fromProto(request));
  }

  private DroneServiceOuterClass.Empty empty() {
    return DroneServiceOuterClass.Empty.newBuilder().build();
  }
}
