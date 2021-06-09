package it.cutecchia.sdp.admin.server.resources;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import it.cutecchia.sdp.admin.server.beans.DroneInfo;
import it.cutecchia.sdp.admin.server.requests.DroneEnterRequest;
import it.cutecchia.sdp.admin.server.responses.DroneEnterResponse;
import it.cutecchia.sdp.admin.server.responses.JsonErrorResponse;
import it.cutecchia.sdp.admin.server.responses.JsonSuccessResponse;
import it.cutecchia.sdp.admin.server.stores.DronesStore;
import it.cutecchia.sdp.admin.server.stores.InMemoryDronesStore;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/drones")
public class DronesResource {

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  public Response addNewDrone(DroneEnterRequest request) {
    Gson gson = new Gson();
    try {
      DronesStore droneStore = InMemoryDronesStore.getInstance();

      // FIXME: Al momento è poco efficiente sta cosa, sarebbe meglio cercare il master dentro il
      // set di droni che mi è stato ritornato invece di lockare quello principale
      DroneInfo newlyAddedDrone = droneStore.addNewDrone(request.getDroneId());
      Set<DroneInfo> drones = droneStore.getRegisteredDrones();

      return Response.ok(
              JsonSuccessResponse.create(
                  gson,
                  new DroneEnterResponse(
                      newlyAddedDrone, drones, droneStore.getMasterDrone().orElse(null))))
          .build();
    } catch (JsonSyntaxException error) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(JsonErrorResponse.create(gson, "Failed to parse json body"))
          .build();
    } catch (DronesStore.DroneIdAlreadyInUse error) {
      return Response.status(Response.Status.CONFLICT)
          .entity(JsonErrorResponse.create(gson, "There already is a drone with that id"))
          .build();
    } catch (Exception error) {
      error.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(JsonErrorResponse.create(gson, "Internal server error"))
          .build();
    }
  }
}
