package com.plantsync.platform.iam.interfaces.rest.transform;

import com.plantsync.platform.iam.domain.model.commands.SignInCommand;
import com.plantsync.platform.iam.interfaces.rest.resources.SignInResource;

/**
 * The type Sign in command from resource assembler.
 */
public class SignInCommandFromResourceAssembler {
  /**
   * To command from resource sign in command.
   *
   * @param signInResource the sign in resource
   * @return the sign in command
   */
  public static SignInCommand toCommandFromResource(SignInResource signInResource) {
    return new SignInCommand(signInResource.email(), signInResource.password());
  }
}
