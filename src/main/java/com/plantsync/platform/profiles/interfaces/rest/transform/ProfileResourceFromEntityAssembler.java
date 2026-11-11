package com.plantsync.platform.profiles.interfaces.rest.transform;

import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.interfaces.rest.resources.ProfileResource;

/**
 * The type Profile resource from entity assembler.
 */
public class ProfileResourceFromEntityAssembler {

  /**
   * To resource from entity profile resource.
   *
   * @param entity the entity
   * @return the profile resource
   */
  public static ProfileResource toResourceFromEntity(Profile entity) {
    return new ProfileResource(
        entity.getId(),
        entity.getPersonName().name(),
        entity.getProfilePictureBase64(),
        entity.getSubscriptionPlan().name(),
        entity.getUserId().value());
  }
}

