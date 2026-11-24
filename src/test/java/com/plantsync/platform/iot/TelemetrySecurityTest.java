package com.plantsync.platform.iot;

import static org.junit.jupiter.api.Assertions.*;
import com.plantsync.platform.iot.security.NodeCredentials;
import com.plantsync.platform.iot.domain.model.commands.CreateReadingCommand;
import com.plantsync.platform.iot.domain.model.entities.SensorReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TelemetrySecurityTest {
  @Test void nodeSecretCannotAuthenticateAnotherNode() throws Exception {
    String secret = "node-one-secret-with-at-least-32-characters";
    String hash = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
        .digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    var credentials = new NodeCredentials(new ObjectMapper(), "{\"ONE\":\"" + hash + "\"}");
    assertDoesNotThrow(() -> credentials.verify("ONE", secret));
    assertThrows(ResponseStatusException.class, () -> credentials.verify("TWO", secret));
    assertThrows(ResponseStatusException.class, () -> credentials.verify("ONE", null));
  }
  @Test void readingTimestampIsUtcRegardlessOfHostZone() {
    TimeZone original = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
      Instant before = Instant.now().minusSeconds(1);
      var reading = new SensorReading(new CreateReadingCommand(1L, 40, 10, 23f, 45));
      Instant actual = reading.getTimestamp().toInstant(ZoneOffset.UTC);
      assertFalse(actual.isBefore(before));
      assertFalse(actual.isAfter(Instant.now().plusSeconds(1)));
    } finally { TimeZone.setDefault(original); }
  }
}
