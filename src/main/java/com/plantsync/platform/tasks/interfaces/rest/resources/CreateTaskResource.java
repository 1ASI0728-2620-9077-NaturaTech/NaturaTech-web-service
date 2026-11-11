package com.plantsync.platform.tasks.interfaces.rest.resources;

public record CreateTaskResource(
    String action,
    String scheduledDate,
    Long plantId,
    Long profileId,
    Integer humidity,
    String notes
) {
  public CreateTaskResource {

  }
}
