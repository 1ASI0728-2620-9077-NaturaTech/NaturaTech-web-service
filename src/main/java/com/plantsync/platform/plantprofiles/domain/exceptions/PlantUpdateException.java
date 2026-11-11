package com.plantsync.platform.plantprofiles.domain.exceptions;

/**
 * The type Plant update exception.
 */
public class PlantUpdateException extends RuntimeException {
  /**
   * Instantiates a new Plant update exception.
   *
   * @param details the details
   */
  public PlantUpdateException(String details) {
    super(String.format("Error while updating plant: %s", details));
  }
}
