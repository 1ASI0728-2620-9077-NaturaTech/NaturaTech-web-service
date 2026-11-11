package com.plantsync.platform.iot.interfaces.rest.resources;

public record SensorReadingResource(
    Long id,
    Long nodeId,
    Integer lightPercent,
    Integer gasPercent,
    Float airTemperature,
    Integer humidityPercent,
    String timestamp
) {
}
