package it.cutecchia.sdp.admin.server;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import io.netty.handler.codec.http.HttpResponseStatus;
import it.cutecchia.sdp.admin.server.messages.DroneEnterRequest;
import it.cutecchia.sdp.admin.server.messages.DroneEnterResponse;
import it.cutecchia.sdp.common.DroneIdentifier;
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
    try {
      WebResource resource = client.resource(getServerEndpoint("/drones/" + droneToEnter.getId()));
      String response =
          resource
              .type(MediaType.APPLICATION_JSON)
              .post(String.class, new DroneEnterRequest(droneToEnter));

      return gson.fromJson(response, DroneEnterResponse.class);
    } catch (UniformInterfaceException e) {
      if (e.getResponse().getStatusInfo().getStatusCode() == HttpResponseStatus.CONFLICT.code()) {
        throw new DroneIdAlreadyInUse();
      }
      throw e;
    }
  }
}
