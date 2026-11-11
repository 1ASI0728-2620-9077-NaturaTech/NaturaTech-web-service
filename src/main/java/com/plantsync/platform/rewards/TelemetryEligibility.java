package com.plantsync.platform.rewards;

import com.plantsync.platform.iot.domain.model.entities.SensorReading;
import java.time.*;
import java.util.*;

/**
 * Rule v1: 30 continuous days, at most 15 minutes between accepted samples, all within stored
 * thresholds.
 */
public final class TelemetryEligibility {
  private TelemetryEligibility() {}

  public static int days(Reward reward, List<SensorReading> source, Instant now) {
    if (source.isEmpty() || now.isBefore(reward.getStartedAt())) return 0;
    var start = reward.getStartedAt();
    var end = now.isAfter(start.plus(Duration.ofDays(30))) ? start.plus(Duration.ofDays(30)) : now;
    var samples =
        source.stream().sorted(Comparator.comparing(SensorReading::getTimestamp)).toList();
    Instant previous = start;
    for (var s : samples) {
      Instant time = s.getTimestamp().toInstant(ZoneOffset.UTC);
      if (time.isBefore(start) || time.isAfter(end)) continue;
      if (Duration.between(previous, time).getSeconds() > 900
          || time.isBefore(previous)
          || s.getHumidityPercent() == null
          || s.getLightPercent() == null
          || s.getHumidityPercent() < reward.getHumidityMin()
          || s.getHumidityPercent() > reward.getHumidityMax()
          || s.getLightPercent() < reward.getLightMin()
          || s.getLightPercent() > 100) return 0;
      previous = time;
    }
    if (Duration.between(previous, end).getSeconds() > 900) return 0;
    return (int) Duration.between(start, end).toDays();
  }
}
