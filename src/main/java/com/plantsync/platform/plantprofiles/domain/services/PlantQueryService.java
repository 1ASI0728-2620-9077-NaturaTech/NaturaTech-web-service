package com.plantsync.platform.plantprofiles.domain.services;

import com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetAllPlantsByProfileIdQuery;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetAllPlantsQuery;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetPlantByIdQuery;
import java.util.List;
import java.util.Optional;

/**
 * The interface Plant query service.
 */
public interface PlantQueryService {

  /**
   * Handle list.
   *
   * @param query the query
   * @return the list
   */
  List<Plant> handle(GetAllPlantsQuery query);

  /**
   * Handle list.
   *
   * @param query the query
   * @return the list
   */
  List<Plant> handle(GetAllPlantsByProfileIdQuery query);

  /**
   * Handle optional.
   *
   * @param query the query
   * @return the optional
   */
  Optional<Plant> handle(GetPlantByIdQuery query);

}
