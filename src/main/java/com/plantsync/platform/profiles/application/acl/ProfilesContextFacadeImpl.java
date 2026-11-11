package com.plantsync.platform.profiles.application.acl;

import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.plantsync.platform.profiles.domain.model.valueobjects.PersonName;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import com.plantsync.platform.profiles.domain.services.ProfileCommandService;
import com.plantsync.platform.profiles.domain.services.ProfileQueryService;
import com.plantsync.platform.profiles.interfaces.acl.ProfilesContextFacade;
import org.springframework.stereotype.Service;

/**
 * Implementation of the ProfilesContextFacade interface.
 * Provides access to profile-related operations from other contexts.
 */
@Service
public class ProfilesContextFacadeImpl implements ProfilesContextFacade {
  private final ProfileCommandService profileCommandService;
  private final ProfileQueryService profileQueryService;

  /**
   * Constructor for ProfilesContextFacadeImpl.
   *
   * @param profileCommandService The profile command service.
   * @param profileQueryService   The profile query service.
   */
  public ProfilesContextFacadeImpl(ProfileCommandService profileCommandService,
                                   ProfileQueryService profileQueryService) {
    this.profileCommandService = profileCommandService;
    this.profileQueryService = profileQueryService;
  }

  @Override
  public Long createProfile(String name, Long userId, String subscriptionPlan) {
    var command = new CreateProfileCommand(
        new PersonName(name),
        SubscriptionPlan.fromString(subscriptionPlan),
        new UserId(userId));
    var profile = profileCommandService.handle(command);
    return profile.map(Profile::getId).orElse(0L);
  }

}