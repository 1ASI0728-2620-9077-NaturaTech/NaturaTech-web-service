package com.plantsync.platform.rewards;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {
  private final WalletService service;

  public record Challenge(String address, long chainId) {}

  public record Verification(String challengeId, String signature) {}

  @PostMapping("/challenges")
  public Object challenge(@RequestBody Challenge request) {
    return service.challenge(request.address(), request.chainId());
  }

  @PostMapping("/verify")
  public Object verify(@RequestBody Verification request) {
    return service.view(service.verify(request.challengeId(), request.signature()));
  }

  @GetMapping
  public ResponseEntity<?> get() {
    var b = service.get();
    return ResponseEntity.ok(b == null ? null : service.view(b));
  }

  @DeleteMapping
  public ResponseEntity<?> delete() {
    service.unlink();
    return ResponseEntity.noContent().build();
  }
}
