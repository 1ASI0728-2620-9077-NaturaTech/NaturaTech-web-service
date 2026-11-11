package com.plantsync.platform.plantprofiles.interfaces.rest.assemblers;

import com.plantsync.platform.plantprofiles.domain.model.commands.UpdatePlantCommand;
import com.plantsync.platform.plantprofiles.domain.model.valueobjects.HumidityLevel;
import com.plantsync.platform.plantprofiles.domain.model.valueobjects.PlantName;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import com.plantsync.platform.plantprofiles.interfaces.rest.resources.UpdatePlantResource;
import java.time.LocalDate;

public class UpdatePlantCommandFromResourceAssembler {

  public static UpdatePlantCommand toCommandFromResource(
      Long plantId, UpdatePlantResource resource) {
    return new UpdatePlantCommand(
        plantId,
        new PlantName(resource.name()),
        resource.species(),
        resource.description(),
        LocalDate.parse(resource.acquisitionDate()),
        HumidityLevel.valueOf(resource.humidity().toUpperCase()),
        LocalDate.parse(resource.nextWateringDate()),
        resource.imageUrl(),
        resource.notificationsEnabled(),
        resource.temperatureThresholdMin(),
        resource.temperatureThresholdMax(),
        resource.lightThresholdMin(),
        resource.humidityThresholdMin(),
        resource.humidityThresholdMax(),
        new ProfileId(resource.profileId())

    );
  }
}
