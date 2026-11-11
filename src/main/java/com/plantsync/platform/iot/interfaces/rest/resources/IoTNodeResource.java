package com.plantsync.platform.iot.interfaces.rest.resources;

public record IoTNodeResource(
    Long id,
    String nodeCode,
    String status,
    Long plantId,
    Long profileId,
    String createdAt
) {
}
