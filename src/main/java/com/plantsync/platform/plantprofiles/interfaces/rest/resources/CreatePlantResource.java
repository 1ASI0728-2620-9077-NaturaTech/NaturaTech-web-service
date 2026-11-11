package com.plantsync.platform.plantprofiles.interfaces.rest.resources;

public record CreatePlantResource(

    String name,
    String species,
    String description,
    String acquisitionDate,
    String humidity,
    String nextWateringDate,
    String imageUrl,
    Boolean notificationsEnabled,
    Float temperatureThresholdMin,
    Float temperatureThresholdMax,
    Float lightThresholdMin,
    Float humidityThresholdMin,
    Float humidityThresholdMax,
    Long profileId

) {

  public CreatePlantResource {

  }

}
