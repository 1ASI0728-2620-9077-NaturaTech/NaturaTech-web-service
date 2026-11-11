package com.plantsync.platform.iam.interfaces.rest;

import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.plantsync.platform.shared.security.ResourceAccess;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {
  private final ResourceAccess access;
  private final ProfileRepository profiles;

  public record SessionResource(Long id, String email, List<String> roles, Long profileId) {}

  @GetMapping
  public SessionResource me() {
    var user = access.currentUser();
    return new SessionResource(
        user.getId(),
        user.getEmail(),
        user.getRoles().stream().map(r -> r.getName().name()).sorted().toList(),
        profiles.findByUserId(new UserId(user.getId())).map(p -> p.getId()).orElse(null));
  }
}
