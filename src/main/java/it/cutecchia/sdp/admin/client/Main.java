package it.cutecchia.sdp.admin.client;

import it.cutecchia.sdp.admin.server.AdminServerClient;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.FleetStats;
import java.text.DateFormat;
import java.util.*;

public class Main {

  private static void printLogo() {
    System.out.println("______  ______   _____   _   _    ___    ______  _____   _   _ ");
    System.out.println("|  _  \\ | ___ \\ |  _  | | \\ | |  / _ \\  |___  / |  _  | | \\ | |");
    System.out.println("| | | | | |_/ / | | | | |  \\| | / /_\\ \\    / /  | | | | |  \\| |");
    System.out.println("| | | | |    /  | | | | | . ` | |  _  |   / /   | | | | | . ` |");
    System.out.println("| |/ /  | |\\ \\  \\ \\_/ / | |\\  | | | | | ./ /___ \\ \\_/ / | |\\  |");
    System.out.println("|___/   \\_| \\_|  \\___/  \\_| \\_/ \\_| |_/ \\_____/  \\___/  \\_| \\_/");
    System.out.println();
  }

  private static void printHelp() {
    System.out.println("Type one of the following commands and press ENTER:");
    System.out.println("quit: Quit this program");
    System.out.println("help: Show this help");
    System.out.println("drones: Show all drones that are currently in the system");
    System.out.println(
        "stats: Show the last N stats that the master drone communicated to the server");
    System.out.println(
        "deliveries: Show the average number of deliveries each drone has made between two dates");
    System.out.println(
        "distance: Show the average kilometers that each drone has travelled between two dates");
  }

  private static Optional<Integer> getPositiveInteger(Scanner scanner, String message) {
    System.out.println(message);
    do {
      int value = scanner.nextInt();
      if (value >= 0) return Optional.of(value);

      System.out.println("Please insert a positive integer");
    } while (true);
  }

  private static Optional<Long> getTimestamp(Scanner scanner, String message) {
    System.out.println(message);
    do {
      long value = scanner.nextLong();
      if (value >= 0) return Optional.of(value);
      System.out.println("Please insert a positive integer");
    } while (true);
  }

  public static void main(String[] args) {
    printLogo();
    printHelp();

    AdminServerClient client = new AdminServerClient("localhost", 1337);
    MenuController controller = new MenuController(client);
    Scanner stdin = new Scanner(System.in);
    boolean quit = false;
    do {
      System.out.print("> ");
      String input = stdin.nextLine();
      if (input.equalsIgnoreCase("quit")) {
        quit = true;
      } else if (input.equalsIgnoreCase("help")) {
        printHelp();
      } else if (input.equalsIgnoreCase("drones")) {
        controller.showRegisteredDrones();
      } else if (input.equalsIgnoreCase("stats")) {
        Optional<Integer> howMany =
            getPositiveInteger(stdin, "How many stats entries should I show? ");
        if (!howMany.isPresent()) continue;
        controller.showRecentStats(Math.min(100, howMany.get()));
      } else if (input.equalsIgnoreCase("deliveries")) {
        Optional<Long> first = getTimestamp(stdin, "Insert the earliest timestamp: ");
        if (!first.isPresent()) continue;
        Optional<Long> second = getTimestamp(stdin, "Insert the latest timestamp: ");
        if (!second.isPresent()) continue;
        controller.showAverageDeliveriesBetweenTimestamps(first.get(), second.get());
      } else if (input.equalsIgnoreCase("distance")) {
        Optional<Long> first = getTimestamp(stdin, "Insert the earliest timestamp: ");
        if (!first.isPresent()) continue;
        Optional<Long> second = getTimestamp(stdin, "Insert the latest timestamp: ");
        if (!second.isPresent()) continue;
        controller.showAverageKmsTravelledBetweenTimestamps(first.get(), second.get());
      }
      System.out.println();
    } while (!quit);
  }

  private static class MenuController {
    private final AdminServerClient client;

    public MenuController(AdminServerClient client) {
      this.client = client;
    }

    public void showRegisteredDrones() {
      Set<DroneIdentifier> drones = client.getRegisteredDrones();
      System.out.printf("There are currently %d drones in the system%n", drones.size());
      System.out.printf("%-10s | %-16s | %-10s %n", "ID", "Address", "RPC Port");
      for (DroneIdentifier drone : drones) {
        System.out.printf(
            "%-10s | %-16s | %-10s %n",
            drone.getId(), drone.getIpAddress(), drone.getConnectionPort());
      }
    }

    public void showAverageDeliveriesBetweenTimestamps(long t1, long t2) {
      long first = Math.min(t1, t2);
      long last = Math.max(t1, t2);

      DateFormat formatter = DateFormat.getDateTimeInstance();
      String formattedFirst = formatter.format(new Date(first));
      String formattedLast = formatter.format(new Date(last));

      double average = client.getAverageDeliveriesBetweenTimestamps(first, last);

      System.out.printf(
          "Between %s and %s each drone completed %f deliveries on average",
          formattedFirst, formattedLast, average);
    }

    public void showAverageKmsTravelledBetweenTimestamps(long t1, long t2) {
      long first = Math.min(t1, t2);
      long last = Math.max(t1, t2);

      DateFormat formatter = DateFormat.getDateTimeInstance();
      String formattedFirst = formatter.format(new Date(first));
      String formattedLast = formatter.format(new Date(last));

      double average = client.getAverageKmsTravelledBetweenTimestamps(first, last);

      System.out.printf(
          "Between %s and %s each drone travelled %f kms on average",
          formattedFirst, formattedLast, average);
    }

    public void showRecentStats(int howMany) {
      DateFormat formatter = DateFormat.getDateTimeInstance();

      List<FleetStats> recentStats = client.getRecentStats(howMany);
      System.out.printf(
          "These are the last %d statistics received from the drones%n",
          Math.min(howMany, recentStats.size()));
      System.out.printf(
          "%-30s | %-10s | %-10s | %-10s | %-10s%n",
          "Timestamp", "Deliveries", "Distance", "Pollution", "Battery");
      for (FleetStats stats : recentStats) {
        String niceTimestamp = formatter.format(new Date(stats.getTimestamp()));
        System.out.printf(
            "%-30s | %10f | %10f | %10f | %10f%%%n",
            niceTimestamp,
            stats.getAverageDeliveries(),
            stats.getAverageKmTravelled(),
            stats.getAveragePollution(),
            stats.getAverageBatteryLevel());
      }
    }
  }
}
