package com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories;

import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Profile} entities.
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

  /**
   * Finds a profile by its associated user ID.
   *
   * @param userId The ID of the user.
   * @return An {@link Optional} containing the {@link Profile} if found.
   */
  Optional<Profile> findByUserId(UserId userId);
}