package com.plantsync.platform.iam.interfaces.rest.transform;

import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;

/**
 * Auth Resource Assembler.
 **/

public class AuthenticatedUserResourceFromEntityAssembler {

  /**
   * Auth User Resource.
   * */

  public static AuthenticatedUserResource toResourceFromEntity(User user, String token) {


    return new AuthenticatedUserResource(user.getId(), user.getEmail(), token);
  }
}
