package com.plantsync.platform.shared.security;

import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository;
import com.plantsync.platform.plantprofiles.infrastructure.persistence.jpa.repositories.PlantRepository;
import com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.plantsync.platform.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Server-side ownership checks shared by legacy and new REST endpoints. */
@Component("access")
@RequiredArgsConstructor
public class ResourceAccess {
  private final UserRepository users;
  private final ProfileRepository profiles;
  private final PlantRepository plants;
  private final TaskRepository tasks;
  private final IotNodeRepository nodes;

  public User currentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated())
      throw new AccessDeniedException("Session required");
    return users
        .findByEmail(auth.getName())
        .orElseThrow(() -> new AccessDeniedException("Session required"));
  }

  public boolean admin() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  public boolean user(Long id) {
    return id != null && (admin() || Objects.equals(currentUser().getId(), id));
  }

  public boolean profile(Long id) {
    return id != null && profiles.findById(id).map(p -> user(p.getUserId().value())).orElse(false);
  }

  public boolean plant(Long id) {
    return id != null
        && plants.findById(id).map(p -> profile(p.getProfileId().value())).orElse(false);
  }

  public boolean plantProfile(Long plantId, Long profileId) {
    return plantId != null
        && profile(profileId)
        && plants
            .findById(plantId)
            .map(p -> Objects.equals(p.getProfileId().value(), profileId))
            .orElse(false);
  }

  public boolean task(Long id) {
    return id != null
        && tasks
            .findById(id)
            .map(t -> plantProfile(t.getPlantId().value(), t.getProfileId().value()))
            .orElse(false);
  }

  public boolean node(String code) {
    return code != null
        && nodes
            .findByNodeCode(code)
            .map(n -> n.getProfileId() != null && profile(n.getProfileId().value()))
            .orElse(false);
  }

  public boolean link(String code, Long plantId) {
    return plantId != null
        && node(code)
        && plants
            .findById(plantId)
            .map(
                p ->
                    nodes
                        .findByNodeCode(code)
                        .map(n -> Objects.equals(n.getProfileId(), p.getProfileId()))
                        .orElse(false))
            .orElse(false);
  }

  public boolean createNode(Long profileId, Long plantId) {
    return profile(profileId) && (plantId == null || plantProfile(plantId, profileId));
  }

  public boolean plan(Long profileId, String requested) {
    return profile(profileId)
        && (admin()
            || requested == null
            || profiles
                .findById(profileId)
                .map(p -> p.getSubscriptionPlan().name().equals(requested))
                .orElse(false));
  }
}
