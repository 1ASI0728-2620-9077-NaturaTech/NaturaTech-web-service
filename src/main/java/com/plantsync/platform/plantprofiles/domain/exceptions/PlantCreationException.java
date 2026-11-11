package com.plantsync.platform.plantprofiles.domain.exceptions;

/**
 * The type Plant creation exception.
 */
public class PlantCreationException extends RuntimeException {
  /**
   * Instantiates a new Plant creation exception.
   *
   * @param details the details
   */
  public PlantCreationException(String details) {
    super(String.format("Error saving plant: %s", details));
  }
}