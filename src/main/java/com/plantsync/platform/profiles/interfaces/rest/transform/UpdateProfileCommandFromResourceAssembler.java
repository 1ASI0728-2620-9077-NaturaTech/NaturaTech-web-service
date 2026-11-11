package com.plantsync.platform.profiles.interfaces.rest.transform;

import com.plantsync.platform.profiles.domain.model.commands.UpdateProfileCommand;
import com.plantsync.platform.profiles.interfaces.rest.resources.UpdateProfileResource;

/**
 * The type Update profile command from resource assembler.
 */
public class UpdateProfileCommandFromResourceAssembler {
  /**
   * To command from resource update profile command.
   *
   * @param id       the id
   * @param resource the resource
   * @return the update profile command
   */
  public static UpdateProfileCommand toCommandFromResource(
      Long id, UpdateProfileResource resource) {
    return new UpdateProfileCommand(
        id,
        resource.personName(),
        resource.subscriptionPlan(),
        resource.profilePictureBase64());
  }
}
