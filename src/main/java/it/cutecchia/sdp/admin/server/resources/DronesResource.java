package it.cutecchia.sdp.admin.server.resources;

import com.google.gson.Gson;
import it.cutecchia.sdp.admin.server.messages.DroneEnterRequest;
import it.cutecchia.sdp.admin.server.messages.DroneEnterResponse;
import it.cutecchia.sdp.admin.server.stores.DronesStore;
import it.cutecchia.sdp.admin.server.stores.InMemoryDronesStore;
import it.cutecchia.sdp.common.CityPoint;
import it.cutecchia.sdp.common.DroneIdentifier;
import java.util.Set;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/drones")
public class DronesResource {

  @POST
  @Path("{droneId}")
  @Consumes("application/json")
  @Produces("application/json")
  public Response addNewDrone(@PathParam("droneId") int droneId, DroneEnterRequest request) {
    Gson gson = new Gson();
    try {
      DronesStore droneStore = InMemoryDronesStore.getInstance();

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
}
