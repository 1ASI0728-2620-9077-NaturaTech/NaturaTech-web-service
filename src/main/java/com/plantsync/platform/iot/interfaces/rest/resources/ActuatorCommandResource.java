package com.plantsync.platform.iot.interfaces.rest.resources;

public record ActuatorCommandResource(
    Long id,
    Long nodeId,
    String actuatorType,
    String action,
    String status,
    String issuedAt
) {
}
