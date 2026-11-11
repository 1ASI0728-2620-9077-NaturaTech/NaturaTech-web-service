package com.plantsync.platform.iam.domain.exceptions;

import com.plantsync.platform.iam.domain.model.valueobjects.Roles;

/**
 * Exception thrown when a role is not found.
 */
public class RoleNotFoundException extends RuntimeException {
  /**
   * Constructor for RoleNotFoundException.
   *
   * @param roleName The role name.
   */
  public RoleNotFoundException(Roles roleName) {
    super(String.format("Role with name '%s' not found.", roleName));
  }
}
