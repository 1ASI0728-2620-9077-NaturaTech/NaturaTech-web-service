package com.plantsync.platform.iam.interfaces.rest.transform;

import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.interfaces.rest.resources.RoleResource;

/**
 * The type Role resource from entity assembler.
 */
public class RoleResourceFromEntityAssembler {
  /**
   * To resource from entity role resource.
   *
   * @param role the role
   * @return the role resource
   */
  public static RoleResource toResourceFromEntity(Role role) {
    return new RoleResource(role.getId(), role.getStringName());
  }
}