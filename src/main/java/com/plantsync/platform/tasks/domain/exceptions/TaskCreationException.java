package com.plantsync.platform.tasks.domain.exceptions;

/**
 * The type Task creation exception.
 */
public class TaskCreationException extends RuntimeException {
  /**
   * Instantiates a new Task creation exception.
   *
   * @param details the details
   */
  public TaskCreationException(String details) {
    super(String.format("Error saving task: %s", details));
  }
}
