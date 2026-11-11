package com.plantsync.platform.tasks.interfaces.rest.resources;

public record TaskResource(
    Long id,
    String action,
    String scheduledDate,
    String completedAt,
    Long plantId,
    Long profileId,
    Integer humidity,
    String notes,
    String status
) {
}
