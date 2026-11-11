package com.plantsync.platform.plantprofiles.domain.exceptions;

/**
 * Exception thrown when a plant is not found with a specific ID.
 */
public class PlantNotFoundException extends RuntimeException {

  /**
   * Constructs a new PlantNotFoundException with the specified plant ID.
   *
   * @param plantId The ID of the plant that was not found.
   */
  public PlantNotFoundException(Long plantId) {
    super(String.format("Plant with ID %s not found.", plantId));
  }
}
