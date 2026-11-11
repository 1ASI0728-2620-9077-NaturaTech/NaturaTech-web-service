package com.plantsync.platform.rewards;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {
  private final RewardService service;

  public record Enrollment(Long plantId) {}

  @PostMapping
  public ResponseEntity<?> enroll(@RequestBody Enrollment request) {
    return ResponseEntity.status(201).body(service.view(service.enroll(request.plantId())));
  }

  @GetMapping
  public Object list() {
    return service.list();
  }

  @PostMapping("/{id}/restart")
  public Object restart(@PathVariable String id) {
    return service.view(service.restart(id));
  }

  @GetMapping("/{id}")
  public Object get(@PathVariable String id) {
    return service.view(service.refresh(id));
  }

  @PostMapping("/{id}/claim")
  public ResponseEntity<?> claim(
      @PathVariable String id, @RequestHeader("Idempotency-Key") String key) {
    return ResponseEntity.accepted().body(service.view(service.claim(id, key)));
  }
}
