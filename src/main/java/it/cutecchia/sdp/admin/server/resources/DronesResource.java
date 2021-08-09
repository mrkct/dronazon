package it.cutecchia.sdp.admin.server.resources;

import com.google.gson.Gson;
import it.cutecchia.sdp.admin.server.messages.DroneEnterRequest;
import it.cutecchia.sdp.admin.server.messages.DroneEnterResponse;
import it.cutecchia.sdp.admin.server.stores.DronesStore;
import it.cutecchia.sdp.admin.server.stores.InMemoryDronesStore;
import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import it.cutecchia.sdp.common.Log;
import java.util.Set;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/drones")
public class DronesResource {

  @GET
  @Produces("application/json")
  public Response getDrones() {
    DronesStore store = InMemoryDronesStore.getInstance();
    return Response.ok(store.getRegisteredDrones()).type(MediaType.APPLICATION_JSON).build();
  }

  @POST
  @Path("{droneId}")
  @Consumes("application/json")
  @Produces("application/json")
  public Response addNewDrone(@PathParam("droneId") int droneId, DroneEnterRequest request) {
    Gson gson = new Gson();
    try {
      DronesStore droneStore = InMemoryDronesStore.getInstance();
      Log.info(
          "New drone: Id=%d, Address=%s, Port=%d",
          droneId, request.getIpAddress(), request.getConnectionPort());
      droneStore.addNewDrone(droneId, request.getIpAddress(), request.getConnectionPort());
      Set<DroneIdentifier> drones = droneStore.getRegisteredDrones();

      return Response.ok(gson.toJson(new DroneEnterResponse(CityPoint.randomPosition(), drones)))
          .type(MediaType.APPLICATION_JSON)
          .build();
    } catch (DronesStore.DroneIdAlreadyInUse error) {
      return Response.status(Response.Status.CONFLICT).build();
    } catch (Exception error) {
      error.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @DELETE
  @Path("{droneId}")
  public Response removeDrone(@PathParam("droneId") int droneId) {
    DronesStore store = InMemoryDronesStore.getInstance();
    try {
      store.removeDroneById(droneId);
      return Response.ok().build();
    } catch (DronesStore.DroneIdNotFound ignored) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }
}
