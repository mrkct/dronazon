package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.Random;
import java.util.Scanner;

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

    public Args() {
      Random random = new Random();
      droneId = random.nextInt();
      rpcListenPort = 1234;
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

  public static void main(String[] cliArgs) throws InterruptedException {
    Args args;
    if (cliArgs.length > 0) {
      args = Args.fromCliArguments(cliArgs);
    } else {
      args = new Args();
    }

    System.out.printf(
        "Starting drone #%d, listening on port %d and communicating with %s:%d...%n",
        args.getDroneId(),
        args.getRpcListenPort(),
        args.getAdminServerAddress(),
        args.getAdminServerPort());

    Drone drone =
        new Drone(
            new DroneIdentifier(args.getDroneId(), "localhost", args.getRpcListenPort()),
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
                System.err.println("wrong");
              }
              keyboard.close();
              drone.shutdown();
            });

    Thread droneThread = new Thread(drone::start);

    userInputThread.start();
    droneThread.start();
    // We intentionally join only the drone thread
    droneThread.join();
  }
}