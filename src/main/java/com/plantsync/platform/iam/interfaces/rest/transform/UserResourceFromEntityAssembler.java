package com.plantsync.platform.iam.interfaces.rest.transform;

import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.interfaces.rest.resources.UserResource;

/**
 * The type User resource from entity assembler.
 */
public class UserResourceFromEntityAssembler {
  /**
   * To resource from entity user resource.
   *
   * @param user the user
   * @return the user resource
   */
  public static UserResource toResourceFromEntity(User user) {
    var roles = user.getRoles().stream().map(Role::getStringName).toList();
    return new UserResource(user.getId(), user.getEmail(), roles);
  }
}
