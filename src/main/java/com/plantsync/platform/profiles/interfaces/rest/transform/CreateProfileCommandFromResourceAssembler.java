package com.plantsync.platform.profiles.interfaces.rest.transform;

import com.plantsync.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.plantsync.platform.profiles.domain.model.valueobjects.PersonName;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import com.plantsync.platform.profiles.interfaces.rest.resources.CreateProfileResource;

/**
 * The type Create profile command from resource assembler.
 */
public class CreateProfileCommandFromResourceAssembler {

  /**
   * To command from resource create profile command.
   *
   * @param resource the resource
   * @return the create profile command
   */
  public static CreateProfileCommand toCommandFromResource(CreateProfileResource resource) {
    return new CreateProfileCommand(
        new PersonName(resource.personName()),
        SubscriptionPlan.valueOf(resource.subscriptionPlan().toUpperCase()),
        new UserId(resource.userId()));
  }
}