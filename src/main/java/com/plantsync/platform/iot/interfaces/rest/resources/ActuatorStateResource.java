package com.plantsync.platform.iot.interfaces.rest.resources;

public record ActuatorStateResource(
    String buzzerMode,
    String servoMode,
    String ledMode
) {
}
