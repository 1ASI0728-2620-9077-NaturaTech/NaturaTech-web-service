package com.plantsync.platform.iot.interfaces.rest.resources;

public record ThresholdsResource(
    Boolean linked,
    Long nodeId,
    Float temperatureThresholdMin,
    Float temperatureThresholdMax,
    Float lightThresholdMin
) {
}
