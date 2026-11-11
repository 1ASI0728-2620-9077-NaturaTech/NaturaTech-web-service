package com.plantsync.platform.plantprofiles.domain.exceptions;

/**
 * Exception thrown when a plant cannot be deleted.
 */
public class PlantDeletionException extends RuntimeException {

  /**
   * Constructs a new PlantDeletionException with the specified details.
   *
   * @param details The error details.
   */
  public PlantDeletionException(String details) {
    super(String.format("Error while deleting plant: %s", details));
  }
}
