package it.cutecchia.sdp.admin.server.resources;

import com.google.gson.Gson;
import it.cutecchia.sdp.admin.server.beans.DroneInfo;
import it.cutecchia.sdp.admin.server.requests.DroneEnterRequest;
import it.cutecchia.sdp.admin.server.responses.DroneEnterResponse;
import it.cutecchia.sdp.admin.server.responses.JsonResponse;
import it.cutecchia.sdp.admin.server.stores.DronesStore;
import it.cutecchia.sdp.admin.server.stores.InMemoryDronesStore;
import java.util.Set;
import javax.ws.rs.*;
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

      // FIXME: Maybe we should return the entire set when we addNewDrone and then search the new
      // drone into that
      DroneInfo newlyAddedDrone =
          droneStore.addNewDrone(droneId, request.getIpAddress(), request.getConnectionPort());
      Set<DroneInfo> drones = droneStore.getRegisteredDrones();

      return Response.ok(
              JsonResponse.success(gson, new DroneEnterResponse(newlyAddedDrone, drones)))
          .build();
    } catch (DronesStore.DroneIdAlreadyInUse error) {
      return Response.status(Response.Status.CONFLICT)
          .entity(JsonResponse.error(gson, "There already is a drone with that id"))
          .build();
    } catch (Exception error) {
      error.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(JsonResponse.error(gson, "Internal server error"))
          .build();
    }
  }
}
