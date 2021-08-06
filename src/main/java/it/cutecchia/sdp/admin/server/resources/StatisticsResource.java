package it.cutecchia.sdp.admin.server.resources;

import it.cutecchia.sdp.admin.server.stores.InMemoryStatisticsStore;
import it.cutecchia.sdp.common.FleetStats;
import it.cutecchia.sdp.common.Log;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/stats")
public class StatisticsResource {

  @GET
  @Produces("application/json")
  @Path("/last/{last}")
  public Response getStatistics(@PathParam("last") int last) {
    List<FleetStats> stats = InMemoryStatisticsStore.getInstance().getMostRecentStats(last);
    return Response.ok(stats).build();
  }

  @GET
  @Produces("application/json")
  @Path("/after/{t1}/before/{t2}")
  public Response getStatsBetweenTimestamps(@PathParam("t1") long t1, @PathParam("t2") long t2) {
    if (t1 > t2) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    return Response.ok(InMemoryStatisticsStore.getInstance().getStatsBetween(t1, t2)).build();
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  public Response postStatistics(FleetStats stats) {
    Log.info("Fleet stats: %s", stats);
    InMemoryStatisticsStore.getInstance().addStatistic(stats);
    return Response.ok().build();
  }
}
