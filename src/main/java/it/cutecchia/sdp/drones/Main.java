package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Main {
  private static class Args {
    private final int droneId;
    private final int rpcListenPort;

    public int getDroneId() {
      return droneId;
    }

    public int getRpcListenPort() {
      return rpcListenPort;
    }

    public String getAdminServerAddress() {
      return adminServerAddress;
    }

    public int getAdminServerPort() {
      return adminServerPort;
    }

    private final String adminServerAddress;
    private final int adminServerPort;

    private static final int MIN_RPC_PORT = 10000;
    private static final int MAX_RPC_PORT = 50000;

    public Args() {
      Random random = new Random();
      droneId = random.nextInt() & Integer.MAX_VALUE;
      rpcListenPort = random.nextInt(MAX_RPC_PORT - MIN_RPC_PORT) + MIN_RPC_PORT;
      adminServerAddress = "localhost";
      adminServerPort = 1337;
    }

    public Args(int droneId, int rpcListenPort, String adminServerAddress, int adminServerPort) {
      this.droneId = droneId;
      this.rpcListenPort = rpcListenPort;
      this.adminServerAddress = adminServerAddress;
      this.adminServerPort = adminServerPort;
    }

    public static Args fromCliArguments(String[] args) {
      Integer droneId = null;
      Integer listenPort = null, adminServerPort = null;
      String adminServerAddress = null;
      try {
        droneId = Integer.parseInt(args[0]);
        listenPort = Integer.parseInt(args[1]);
        adminServerAddress = args[2];
        adminServerPort = Integer.parseInt(args[3]);
      } catch (NumberFormatException ignored) {
        System.err.println("Wrong format for argument values");
        System.exit(-1);
      }

      return new Args(droneId, listenPort, adminServerAddress, adminServerPort);
    }
  }

  public static void main(String[] cliArgs) throws InterruptedException, MqttException {
    Args args;
    if (cliArgs.length > 0) {
      args = Args.fromCliArguments(cliArgs);
    } else {
      args = new Args();
    }

    Log.notice(
        "Starting drone #%d, listening on port %d and communicating with %s:%d...%n",
        args.getDroneId(),
        args.getRpcListenPort(),
        args.getAdminServerAddress(),
        args.getAdminServerPort());

    Drone drone =
        new Drone(
            new DroneIdentifier(args.getDroneId(), "localhost", args.getRpcListenPort()),
            new MqttOrderSource("mqtt.mrkct.xyz", 8000, "dronazon/smartcity/orders"),
            new AdminServerClient(args.getAdminServerAddress(), args.getAdminServerPort()));

    Thread userInputThread =
        new Thread(
            () -> {
              Scanner keyboard = new Scanner(System.in);
              while (true) {
                while (!keyboard.hasNextLine()) {}
                if ("quit".equalsIgnoreCase(keyboard.nextLine().trim())) {
                  break;
                }
              }
              keyboard.close();
              drone.shutdown();
            });

    Thread droneThread =
        new Thread(
            () -> {
              try {
                drone.start();
              } catch (IOException e) {
                e.printStackTrace();
              }
            });

    userInputThread.start();
    droneThread.start();
    // We intentionally join only the drone thread
    droneThread.join();
  }
}
