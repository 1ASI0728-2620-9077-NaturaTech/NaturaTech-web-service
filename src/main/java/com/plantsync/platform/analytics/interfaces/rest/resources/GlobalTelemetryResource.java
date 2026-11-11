package com.plantsync.platform.analytics.interfaces.rest.resources;

public record GlobalTelemetryResource(
    Double averageLightPercent,
    Double averageAirTemperature,
    Double averageGasPercent,
    Double averageHumidityPercent
) {
}
