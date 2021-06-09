package it.cutecchia.sdp.admin.server.resources;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import it.cutecchia.sdp.admin.server.beans.Statistics;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/stats")
public class StatisticsResource {

  @GET
  @Produces("application/json")
  public Response getStatistics() {
    Statistics s = Statistics.getMostRecentStatistics();
    Gson gson = new Gson();
    return Response.ok(gson.toJson(s)).build();
  }

  // FIXME: Instead of POST use UPDATE
  @POST
  @Consumes("application/json")
  @Produces("application/json")
  public Response updateStatistics(String statisticsJson) {
    Gson gson = new Gson();
    try {
      Statistics updatedStatistics = gson.fromJson(statisticsJson, Statistics.class);
      Statistics.updateStatistics(updatedStatistics);

      return Response.ok(gson.toJson(updatedStatistics)).build();
    } catch (JsonSyntaxException | NumberFormatException ignored) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }
}
