package com.plantsync.platform.intelligence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AgentController {
  private final AgentService service;
  private final ObjectMapper json;

  public record Message(String content, Long plantId) {}

  @PostMapping("/agent/conversations")
  public ResponseEntity<?> create() {
    return ResponseEntity.status(201).body(Map.of("id", service.create().getId()));
  }

  @GetMapping("/agent/conversations/{id}")
  public Object get(@PathVariable String id) throws Exception {
    var c = service.ownConversation(id);
    return Map.of("id", c.getId(), "messages", json.readTree(c.getMessagesJson()));
  }

  @PostMapping("/agent/conversations/{id}/messages")
  public Object message(@PathVariable String id, @RequestBody Message body) {
    return service.message(id, body.content(), body.plantId());
  }

  @GetMapping("/actuator-command-proposals/{id}")
  public Object proposal(@PathVariable String id) {
    return service.view(service.ownProposal(id));
  }

  @PostMapping("/actuator-command-proposals/{id}/confirm")
  public ResponseEntity<?> confirm(
      @PathVariable String id, @RequestHeader("Idempotency-Key") String key) {
    return ResponseEntity.accepted().body(service.confirm(id, key));
  }

  @PostMapping("/actuator-command-proposals/{id}/cancel")
  public Object cancel(@PathVariable String id) {
    return service.cancel(id);
  }

  @GetMapping("/actuator-commands/{id}")
  public Object command(@PathVariable String id) {
    return service.command(service.ownProposal(id));
  }
}
