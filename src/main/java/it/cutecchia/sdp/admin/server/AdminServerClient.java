package it.cutecchia.sdp.admin.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import io.netty.handler.codec.http.HttpResponseStatus;
import it.cutecchia.sdp.admin.server.messages.DroneEnterRequest;
import it.cutecchia.sdp.admin.server.messages.DroneEnterResponse;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.FleetStats;
import it.cutecchia.sdp.common.Log;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.core.MediaType;

public class AdminServerClient {
  private final Client client;
  private final String serverAddress;
  private final int port;
  private final Gson gson = new Gson();

  public AdminServerClient(String serverAddress, int port) {
    this.client = Client.create();
    this.serverAddress = serverAddress;
    this.port = port;
  }

  private String getServerEndpoint(String endpoint, Object... args) {
    if (endpoint.length() > 0 && endpoint.charAt(0) == '/') {
      endpoint = endpoint.substring(1);
    }
    return String.format("http://%s:%d/%s", serverAddress, port, String.format(endpoint, args));
  }

  public static class DroneIdAlreadyInUse extends Exception {}

  public DroneEnterResponse requestDroneToEnter(DroneIdentifier droneToEnter)
      throws DroneIdAlreadyInUse {
    Log.info("AdminServerClient, droneToEnter=%s", droneToEnter);
    try {
      WebResource resource = client.resource(getServerEndpoint("/drones/" + droneToEnter.getId()));
      String response =
          resource
              .type(MediaType.APPLICATION_JSON)
              .post(String.class, gson.toJson(new DroneEnterRequest(droneToEnter)));
      Log.info("REST Response: %s", response);
      return gson.fromJson(response, DroneEnterResponse.class);
    } catch (UniformInterfaceException e) {
      if (e.getResponse().getStatusInfo().getStatusCode() == HttpResponseStatus.CONFLICT.code()) {
        throw new DroneIdAlreadyInUse();
      }
      throw e;
    }
  }

  public void requestDroneExit(DroneIdentifier quittingDrone) {
    WebResource resource =
        client.resource(getServerEndpoint(String.format("/drones/%d", quittingDrone.getId())));
    resource.delete();
  }

  public void sendFleetStats(FleetStats stats) {
    Log.info("Sending stats to admin server...");

    WebResource resource = client.resource(getServerEndpoint("/stats"));

    // FIXME: If I return the object directly on the server the json is just '{}'
    Gson gson = new Gson();
    resource.type(MediaType.APPLICATION_JSON).post(gson.toJson(stats));
  }

  public Set<DroneIdentifier> getRegisteredDrones() {
    WebResource resource = client.resource(getServerEndpoint("/drones"));

    Gson gson = new Gson();
    Type droneListType = new TypeToken<TreeSet<DroneIdentifier>>() {}.getType();
    String jsonListOfDrones = resource.type(MediaType.APPLICATION_JSON).get(String.class);
    return gson.fromJson(jsonListOfDrones, droneListType);
  }

  public double getAverageDeliveriesBetweenTimestamps(long t1, long t2) {
    WebResource resource =
        client.resource(getServerEndpoint("/stats/average-deliveries/after/%d/before/%d", t1, t2));

    String json = resource.type(MediaType.APPLICATION_JSON).get(String.class);
    Gson gson = new Gson();
    return gson.fromJson(json, Double.class);
  }

  public double getAverageKmsTravelledBetweenTimestamps(long t1, long t2) {
    WebResource resource =
        client.resource(
            getServerEndpoint("/stats/average-kms-travelled/after/%d/before/%d", t1, t2));

    String json = resource.type(MediaType.APPLICATION_JSON).get(String.class);
    Gson gson = new Gson();
    return gson.fromJson(json, Double.class);
  }

  public List<FleetStats> getRecentStats(int howMany) {
    WebResource resource = client.resource(getServerEndpoint("/stats/last/%d", howMany));

    Gson gson = new Gson();
    Type statsListType = new TypeToken<ArrayList<FleetStats>>() {}.getType();
    String jsonListOfStats = resource.type(MediaType.APPLICATION_JSON).get(String.class);
    return gson.fromJson(jsonListOfStats, statsListType);
  }
}
