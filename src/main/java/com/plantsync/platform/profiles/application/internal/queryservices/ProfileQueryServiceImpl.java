package com.plantsync.platform.profiles.application.internal.queryservices;

import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import com.plantsync.platform.profiles.domain.services.ProfileQueryService;
import com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service implementation for querying profile entities.
 */
@Service
public class ProfileQueryServiceImpl implements ProfileQueryService {
  private final ProfileRepository profileRepository;

  /**
   * Constructor for ProfileQueryServiceImpl.
   *
   * @param profileRepository The {@link ProfileRepository} repository.
   */
  public ProfileQueryServiceImpl(ProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  // inherited javadoc
  @Override
  public Optional<Profile> handle(GetProfileByIdQuery query) {
    return profileRepository.findById(query.profileId());
  }

  // inherited javadoc
  @Override
  public List<Profile> handle(GetAllProfilesQuery query) {
    return profileRepository.findAll();
  }

  @Override
  public Optional<Profile> handle(GetProfileByUserIdQuery query) {
    return profileRepository.findByUserId(new UserId(query.userId()));
  }
}

