package com.plantsync.platform.iot.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateReadingResource(
    Long nodeId,
    @Min(0) @Max(100) Integer lightPercent,
    @Min(0) @Max(100) Integer gasPercent,
    @Min(-40) @Max(80) Float airTemperature,
    @Min(0) @Max(100) Integer humidityPercent
) {
}
