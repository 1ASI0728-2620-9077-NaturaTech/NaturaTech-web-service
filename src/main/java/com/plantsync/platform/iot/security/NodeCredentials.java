package com.plantsync.platform.iot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Component
public class NodeCredentials {
  private final ObjectMapper json;
  private final String hashes;
  public NodeCredentials(ObjectMapper json,
      @Value("${integrations.iot.node-key-hashes:{}}") String hashes) {
    this.json = json;
    this.hashes = hashes;
  }
  public void verify(String node, String key) {
    try {
      if (key == null || key.length() < 32) throw new IllegalArgumentException();
      String expected = json.readTree(hashes).path(node).asText();
      String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(key.getBytes(StandardCharsets.UTF_8)));
      if (expected.length() != 64 || !MessageDigest.isEqual(
          expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8)))
        throw new IllegalArgumentException();
    } catch (Exception e) {
      throw new ResponseStatusException(FORBIDDEN, "DEVICE_AUTH_FAILED");
    }
  }
}
