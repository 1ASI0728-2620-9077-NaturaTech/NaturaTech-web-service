package com.plantsync.platform.intelligence;

import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Gateway authentication is additionally required by the existing edge security filter. */
@RestController
@RequestMapping("/api/v1/iot/edge/nodes/{nodeCode}/agent-commands")
@RequiredArgsConstructor
public class AgentDeviceController {
  private final ActionProposalRepository proposals;
  private final EntityManager em;
  private final ObjectMapper json;

  @Value("${integrations.iot.node-key-hashes:{}}")
  private String keyHashes;

  private void authenticateNode(String node, String key) {
    try {
      if (key == null || key.length() < 32) throw new IllegalArgumentException();
      var expected = json.readTree(keyHashes).path(node).asText();
      var actual =
          HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      if (expected.length() != 64
          || !java.security.MessageDigest.isEqual(expected.getBytes(), actual.getBytes()))
        throw new IllegalArgumentException();
    } catch (Exception e) {
      throw new ResponseStatusException(FORBIDDEN, "DEVICE_AUTH_FAILED");
    }
  }

  @GetMapping
  @Transactional
  public Object poll(@PathVariable String nodeCode, @RequestHeader("X-Node-Key") String key) {
    authenticateNode(nodeCode, key);
    var result = new ArrayList<Map<String, Object>>();
    for (var p : proposals.findByNodeCodeAndCommandStatusIn(nodeCode, List.of("QUEUED", "SENT"))) {
      em.lock(p, LockModeType.PESSIMISTIC_WRITE);
      if (!p.getCommandExpiresAt().isAfter(Instant.now())) continue;
      p.setCommandStatus("SENT");
      result.add(
          Map.of(
              "commandId",
              p.getId(),
              "actuator",
              p.getActuator(),
              "durationSeconds",
              p.getDurationSeconds(),
              "expiresAt",
              p.getCommandExpiresAt()));
    }
    return result;
  }

  public record Receipt(String status) {}

  @PostMapping("/{id}/receipt")
  @Transactional
  public Object receipt(
      @PathVariable String nodeCode,
      @PathVariable String id,
      @RequestHeader("X-Node-Key") String key,
      @RequestBody Receipt receipt) {
    authenticateNode(nodeCode, key);
    var p = em.find(ActionProposal.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (p == null || !nodeCode.equals(p.getNodeCode()) || p.getCommandStatus() == null)
      throw new ResponseStatusException(NOT_FOUND, "COMMAND_NOT_FOUND");
    if (receipt.status() == null
        || !Set.of("ACKNOWLEDGED", "EXECUTED", "FAILED").contains(receipt.status()))
      throw new ResponseStatusException(BAD_REQUEST, "RECEIPT_INVALID");
    if (p.getCommandStatus().equals(receipt.status()))
      return Map.of("commandId", id, "status", p.getCommandStatus());
    if (Set.of("EXECUTED", "FAILED").contains(p.getCommandStatus()))
      throw new ResponseStatusException(CONFLICT, "COMMAND_TERMINAL");
    if (p.getCommandExpiresAt().plusSeconds(p.getDurationSeconds() + 60L).isBefore(Instant.now()))
      throw new ResponseStatusException(CONFLICT, "RECEIPT_TOO_LATE");
    if (!Set.of("SENT", "ACKNOWLEDGED").contains(p.getCommandStatus()))
      throw new ResponseStatusException(CONFLICT, "COMMAND_NOT_DELIVERED");
    p.setCommandStatus(receipt.status());
    return Map.of("commandId", id, "status", p.getCommandStatus());
  }
}
