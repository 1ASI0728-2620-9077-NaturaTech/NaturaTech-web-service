package com.plantsync.platform.iot.interfaces.rest.resources;

public record NodeSyncConfigResource(
    Boolean linked,
    Long nodeId,
    String species,
    Float temperatureThresholdMin,
    Float temperatureThresholdMax,
    Float humidityThresholdMin,
    Float humidityThresholdMax,
    Float lightThresholdMin
) {
}
