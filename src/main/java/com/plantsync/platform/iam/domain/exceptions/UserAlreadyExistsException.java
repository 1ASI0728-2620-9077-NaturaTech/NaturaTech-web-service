package com.plantsync.platform.iam.domain.exceptions;

/**
 * Exception thrown when a user already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {
  /**
   * Constructor for UserAlreadyExistsException.
   *
   * @param email The email.
   */
  public UserAlreadyExistsException(String email) {
    super(String.format("User with email '%s' already exists.", email));
  }
}
