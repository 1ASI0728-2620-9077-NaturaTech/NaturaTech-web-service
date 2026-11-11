package com.plantsync.platform.plantprofiles.infrastructure.persistence.jpa.repositories;

import com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Plant} entities.
 */

@Repository
public interface PlantRepository extends JpaRepository<Plant, Long> {

  /**
   * Finds all plants associated with a specific profile ID.
   *
   * @param profileId The ID of the owner profile.
   * @return A list of {@link Plant} entities.
   */
  List<Plant> findByProfileId(ProfileId profileId);

}
