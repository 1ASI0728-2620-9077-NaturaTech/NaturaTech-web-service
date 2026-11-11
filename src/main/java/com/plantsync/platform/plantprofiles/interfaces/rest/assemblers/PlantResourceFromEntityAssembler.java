package com.plantsync.platform.plantprofiles.interfaces.rest.assemblers;

import com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant;
import com.plantsync.platform.plantprofiles.interfaces.rest.resources.PlantResource;

public class PlantResourceFromEntityAssembler {

  public static PlantResource toResourceFromEntity(Plant entity) {

    return new PlantResource(
        entity.getId(),
        entity.getName().value(),
        entity.getSpecies(),
        entity.getDescription(),
        entity.getAcquisitionDate().toString(),
        entity.getHumidity().name(),
        entity.getNextWateringDate().toString(),
        entity.getImageUrl(),
        entity.getNotificationsEnabled(),
        entity.getTemperatureThresholdMin(),
        entity.getTemperatureThresholdMax(),
        entity.getLightThresholdMin(),
        entity.getHumidityThresholdMin(),
        entity.getHumidityThresholdMax(),
        entity.getProfileId().value()
    );
  }
}
