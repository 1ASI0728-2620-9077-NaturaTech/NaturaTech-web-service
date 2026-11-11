package com.plantsync.platform.iot.interfaces.rest.resources;

public record NodeSyncStatusResource(
    Boolean linked,
    ActuatorStateResource desiredState
) {
}
