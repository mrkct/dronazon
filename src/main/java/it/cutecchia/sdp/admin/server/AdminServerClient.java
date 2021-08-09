package it.cutecchia.sdp.admin.server;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import io.netty.handler.codec.http.HttpResponseStatus;
import it.cutecchia.sdp.admin.server.messages.DroneEnterRequest;
import it.cutecchia.sdp.admin.server.messages.DroneEnterResponse;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.FleetStats;
import it.cutecchia.sdp.common.Log;
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

  private String getServerEndpoint(String endpoint) {
    if (endpoint.length() > 0 && endpoint.charAt(0) == '/') {
      endpoint = endpoint.substring(1);
    }
    return String.format("http://%s:%d/%s", serverAddress, port, endpoint);
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
}
