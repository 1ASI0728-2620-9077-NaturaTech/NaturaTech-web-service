package com.plantsync.platform.profiles.domain.exceptions;

/**
 * The type Profile update exception.
 */
public class ProfileUpdateException extends RuntimeException {
  /**
   * Instantiates a new Profile update exception.
   *
   * @param message the message
   */
  public ProfileUpdateException(String message) {
    super("Error updating profile: " + message);
  }
}
