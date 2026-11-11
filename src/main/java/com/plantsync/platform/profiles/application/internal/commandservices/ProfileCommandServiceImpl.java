package com.plantsync.platform.profiles.application.internal.commandservices;

import com.plantsync.platform.profiles.domain.exceptions.ProfileUpdateException;
import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.plantsync.platform.profiles.domain.model.commands.UpdateProfileCommand;
import com.plantsync.platform.profiles.domain.model.valueobjects.PersonName;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.services.ProfileCommandService;
import com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Service implementation for handling profile-related commands. */
@Service
public class ProfileCommandServiceImpl implements ProfileCommandService {
  private final ProfileRepository profileRepository;

  /**
   * Constructor for ProfileCommandServiceImpl.
   *
   * @param profileRepository The {@link ProfileRepository} repository.
   */
  public ProfileCommandServiceImpl(ProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  // inherited javadoc
  @Override
  public Optional<Profile> handle(CreateProfileCommand command) {
    var profile = new Profile(command);
    profileRepository.save(profile);
    return Optional.of(profile);
  }

  @Override
  public Optional<Profile> handle(UpdateProfileCommand command) {
    var result = profileRepository.findById(command.id());

    if (result.isEmpty()) {
      throw new IllegalArgumentException("Profile is empty");
    }
    var profileToUpdate = result.get();

    try {
      var updatedProfile =
          profileRepository.save(
              profileToUpdate.updateInformation(
                  command.personName() == null
                      ? profileToUpdate.getPersonName()
                      : new PersonName(command.personName()),
                  command.subscriptionPlan() == null
                      ? profileToUpdate.getSubscriptionPlan()
                      : SubscriptionPlan.valueOf(command.subscriptionPlan().toUpperCase()),
                  command.profilePictureBase64() == null
                      ? profileToUpdate.getProfilePictureBase64()
                      : command.profilePictureBase64()));
      return Optional.of(updatedProfile);
    } catch (Exception e) {
      throw new ProfileUpdateException(e.getMessage());
    }
  }
}
