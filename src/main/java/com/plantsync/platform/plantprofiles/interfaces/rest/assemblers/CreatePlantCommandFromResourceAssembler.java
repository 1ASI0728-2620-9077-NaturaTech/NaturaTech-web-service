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
        parseDate(resource.acquisitionDate(), false),
        HumidityLevel.valueOf(resource.humidity().toUpperCase()),
        parseDate(resource.nextWateringDate(), true),
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

  private static LocalDate parseDate(String value, boolean optional) {
    if (optional && (value == null || value.isBlank())) return null;
    try {
      return LocalDate.parse(value);
    } catch (java.time.DateTimeException | NullPointerException error) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "PLANT_DATE_INVALID");
    }
  }

}
