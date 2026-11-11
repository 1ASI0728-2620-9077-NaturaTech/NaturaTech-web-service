package com.plantsync.platform.plantprofiles.application.internal.queryservices;

import com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetAllPlantsByProfileIdQuery;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetAllPlantsQuery;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetPlantByIdQuery;
import com.plantsync.platform.plantprofiles.domain.services.PlantQueryService;
import com.plantsync.platform.plantprofiles.infrastructure.persistence.jpa.repositories.PlantRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service implementation for querying plant entities.
 */
@Service
public class PlantQueryServiceImpl implements PlantQueryService {

  private final PlantRepository plantRepository;

  public PlantQueryServiceImpl(PlantRepository plantRepository) {
    this.plantRepository = plantRepository;
  }

  @Override
  public List<Plant> handle(GetAllPlantsQuery query) {
    return plantRepository.findAll();
  }

  @Override
  public List<Plant> handle(GetAllPlantsByProfileIdQuery query) {
    return plantRepository.findByProfileId(query.profileId());
  }

  @Override
  public Optional<Plant> handle(GetPlantByIdQuery query) {
    if (!plantRepository.existsById(query.plantId())) {
      return Optional.empty();
    }
    return plantRepository.findById(query.plantId());
  }
}
