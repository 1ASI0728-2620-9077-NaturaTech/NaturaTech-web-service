package com.plantsync.platform.tasks.domain.exceptions;

public class TaskNotFoundException extends RuntimeException {
  public TaskNotFoundException(Long taskId) {
    super("Task with id " + taskId + " not found");
  }
}
