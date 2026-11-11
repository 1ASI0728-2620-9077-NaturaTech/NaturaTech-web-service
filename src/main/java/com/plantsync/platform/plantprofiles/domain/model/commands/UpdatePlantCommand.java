package com.plantsync.platform.plantprofiles.domain.model.commands;

import com.plantsync.platform.plantprofiles.domain.model.valueobjects.HumidityLevel;
import com.plantsync.platform.plantprofiles.domain.model.valueobjects.PlantName;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import java.time.LocalDate;

/**
 * The type Update plant command.
 */
public record UpdatePlantCommand(
    Long plantId,
    PlantName name,
    String species,
    String description,
    LocalDate acquisitionDate,
    HumidityLevel humidity,
    LocalDate nextWateringDate,
    String imageUrl,
    Boolean notificationsEnabled,
    Float temperatureThresholdMin,
    Float temperatureThresholdMax,
    Float lightThresholdMin,
    Float humidityThresholdMin,
    Float humidityThresholdMax,
    ProfileId profileId
) {
  /**
   * Instantiates a new Update plant command.
   *
   * @param plantId              the plant id
   * @param name                 the name
   * @param species              the species
   * @param description          the description
   * @param acquisitionDate      the acquisition date
   * @param humidity             the humidity
   * @param nextWateringDate     the next watering date
   * @param imageUrl             the image url
   * @param notificationsEnabled the notifications enabled
   * @param profileId            the profile id
   */
  public UpdatePlantCommand {


  }
}
