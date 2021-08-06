package it.cutecchia.sdp.admin.server.stores;

import it.cutecchia.sdp.common.FleetStats;
import java.util.List;

public interface StatisticsStore {
  void addStatistic(FleetStats stats);

  List<FleetStats> getMostRecentStats(int howMany);

  List<FleetStats> getStatsBetween(long earliestTimestamp, long latestTimestamp);
}
