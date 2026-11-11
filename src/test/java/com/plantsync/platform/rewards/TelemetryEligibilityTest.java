package com.plantsync.platform.rewards;

import static org.junit.jupiter.api.Assertions.*;

import com.plantsync.platform.iot.domain.model.entities.SensorReading;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TelemetryEligibilityTest {
  Reward reward() {
    var r = new Reward();
    r.setStartedAt(Instant.parse("2026-01-01T00:00:00Z"));
    r.setHumidityMin(30f);
    r.setHumidityMax(70f);
    r.setLightMin(20f);
    return r;
  }

  List<SensorReading> samples(Reward r, int days) {
    var result = new ArrayList<SensorReading>();
    for (int i = 0; i <= days * 24 * 6; i++) {
      var s = new SensorReading();
      s.setTimestamp(
          LocalDateTime.ofInstant(r.getStartedAt().plusSeconds(i * 600L), ZoneOffset.UTC));
      s.setHumidityPercent(50);
      s.setLightPercent(40);
      result.add(s);
    }
    return result;
  }

  @Test
  void requiresThirtyDaysOfContinuousValidReadings() {
    var r = reward();
    assertEquals(
        30,
        TelemetryEligibility.days(r, samples(r, 30), r.getStartedAt().plus(Duration.ofDays(30))));
    assertEquals(
        29,
        TelemetryEligibility.days(r, samples(r, 29), r.getStartedAt().plus(Duration.ofDays(29))));
  }

  @Test
  void gapOrInvalidReadingDoesNotEarnBadge() {
    var r = reward();
    var readings = samples(r, 30);
    readings.remove(10);
    assertEquals(
        0, TelemetryEligibility.days(r, readings, r.getStartedAt().plus(Duration.ofDays(30))));
    readings = samples(r, 30);
    readings.get(10).setHumidityPercent(99);
    assertEquals(
        0, TelemetryEligibility.days(r, readings, r.getStartedAt().plus(Duration.ofDays(30))));
  }

  @Test
  void oldMissingOrFutureDataDoesNotEarnBadge() {
    var r = reward();
    assertEquals(0, TelemetryEligibility.days(r, List.of(), Instant.now()));
    assertEquals(
        0,
        TelemetryEligibility.days(r, samples(r, 29), r.getStartedAt().plus(Duration.ofDays(30))));
  }
}
