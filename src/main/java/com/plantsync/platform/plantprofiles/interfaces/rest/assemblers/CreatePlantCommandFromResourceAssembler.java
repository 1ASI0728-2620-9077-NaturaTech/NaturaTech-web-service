package com.plantsync.platform.plantprofiles.interfaces.rest.assemblers;

import com.plantsync.platform.plantprofiles.domain.model.commands.CreatePlantCommand;
import com.plantsync.platform.plantprofiles.domain.model.valueobjects.HumidityLevel;
import com.plantsync.platform.plantprofiles.domain.model.valueobjects.PlantName;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import com.plantsync.platform.plantprofiles.interfaces.rest.resources.CreatePlantResource;
import java.time.LocalDate;

public class CreatePlantCommandFromResourceAssembler {

  public static CreatePlantCommand toCommandFromResource(CreatePlantResource resource) {

    return new CreatePlantCommand(
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
