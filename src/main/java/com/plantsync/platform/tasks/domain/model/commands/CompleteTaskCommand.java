package com.plantsync.platform.tasks.domain.model.commands;

public record CompleteTaskCommand(
    Long taskId,
    Integer humidity,
    String notes
) {
  public CompleteTaskCommand {

  }
}
