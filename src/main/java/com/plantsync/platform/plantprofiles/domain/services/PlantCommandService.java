package com.plantsync.platform.plantprofiles.domain.services;

import com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant;
import com.plantsync.platform.plantprofiles.domain.model.commands.CreatePlantCommand;
import com.plantsync.platform.plantprofiles.domain.model.commands.DeletePlantCommand;
import com.plantsync.platform.plantprofiles.domain.model.commands.UpdatePlantCommand;
import java.util.Optional;

/**
 * The interface Plant command service.
 */
public interface PlantCommandService {

  /**
   * Handle long.
   *
   * @param command the command
   * @return the long
   */
  Long handle(CreatePlantCommand command);

  /**
   * Handle.
   *
   * @param command the command
   */
  void handle(DeletePlantCommand command);

  /**
   * Handle optional.
   *
   * @param command the command
   * @return the optional
   */
  Optional<Plant> handle(UpdatePlantCommand command);

}
