package com.plantsync.platform.profiles.domain.exceptions;

/**
 * Exception thrown when a profile is not found.
 */
public class ProfileNotFoundException extends RuntimeException {

  /**
   * Constructs a new ProfileNotFoundException with a detail message.
   *
   * @param message The detail message.
   */
  public ProfileNotFoundException(String message) {
    super("Error updating profile: " + message);
  }
}
