package com.plantsync.platform.iam.interfaces.rest.transform;

import com.plantsync.platform.iam.domain.model.commands.UpdateUserCommand;
import com.plantsync.platform.iam.interfaces.rest.resources.UpdateUserResource;

/**
 * The type Update user command from resource assembler.
 */
public class UpdateUserCommandFromResourceAssembler {

  /**
   * To command from resource update user command.
   *
   * @param id       the id
   * @param resource the resource
   * @return the update user command
   */
  public static UpdateUserCommand toCommandFromResource(Long id, UpdateUserResource resource) {
    return new UpdateUserCommand(id, resource.email());
  }
}