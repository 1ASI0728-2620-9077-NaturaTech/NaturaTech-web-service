package com.plantsync.platform.tasks.domain.exceptions;

/**
 * The type Task deletion exception.
 */
public class TaskDeletionException extends RuntimeException {
  /**
   * Instantiates a new Task deletion exception.
   *
   * @param details the details
   */
  public TaskDeletionException(String details) {
    super(String.format("Error while deleting task: %s", details));
  }
}
