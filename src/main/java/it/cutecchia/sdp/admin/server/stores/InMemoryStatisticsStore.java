package it.cutecchia.sdp.admin.server.stores;

import it.cutecchia.sdp.common.FleetStats;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class InMemoryStatisticsStore implements StatisticsStore {

  private static InMemoryStatisticsStore instance = null;

  private final SortedSet<FleetStats> stats = new TreeSet<>();

  public static synchronized InMemoryStatisticsStore getInstance() {
    if (instance == null) {
      instance = new InMemoryStatisticsStore();
    }
    return instance;
  }

  @Override
  public synchronized void addStatistic(FleetStats newStats) {
    stats.add(newStats);
  }

  @Override
  public List<FleetStats> getMostRecentStats(int howMany) {
    return stats.stream().skip(Math.max(stats.size() - howMany, 0)).collect(Collectors.toList());
  }

  @Override
  public List<FleetStats> getStatsBetween(long earliestTimestamp, long latestTimestamp) {
    return stats.stream()
        .filter(s -> earliestTimestamp <= s.getTimestamp() && s.getTimestamp() <= latestTimestamp)
        .collect(Collectors.toList());
  }
}
