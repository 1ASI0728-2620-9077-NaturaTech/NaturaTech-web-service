package com.plantsync.platform.profiles.domain.services;

import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.plantsync.platform.profiles.domain.model.commands.UpdateProfileCommand;
import java.util.Optional;

/**
 * Service interface for handling profile-related commands.
 */
public interface ProfileCommandService {

  /**
   * Handles the creation of a new profile.
   *
   * @param command The command containing profile data.
   * @return An {@link Optional} containing the created {@link Profile}.
   */
  Optional<Profile> handle(CreateProfileCommand command);

  /**
   * Handles the update of an existing profile.
   *
   * @param command The command containing updated profile data.
   * @return An {@link Optional} containing the updated {@link Profile}.
   */
  Optional<Profile> handle(UpdateProfileCommand command);
}
