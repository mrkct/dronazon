package it.cutecchia.sdp.drones;

public class Main {
  public static void main(String[] args) {
    if (args.length < 4) {
      System.err.println(
          "You need to pass <drone id> <listen port> <admin server ip> <admin server port>");
      System.exit(-1);
    }

    Long droneId = null;
    Integer listenPort = null, adminServerPort = null;
    String adminServerAddress = null;
    try {
      droneId = Long.parseLong(args[0]);
      listenPort = Integer.parseInt(args[1]);
      adminServerAddress = args[2];
      adminServerPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException ignored) {
      System.err.println("Wrong format for argument values");
      System.exit(-1);
    }

    System.out.printf(
        "Starting drone #%d, listening on port %d and communicating with %s:%d...",
        droneId, listenPort, adminServerAddress, adminServerPort);
    new Drone(droneId, listenPort, adminServerAddress, adminServerPort);
  }
}
