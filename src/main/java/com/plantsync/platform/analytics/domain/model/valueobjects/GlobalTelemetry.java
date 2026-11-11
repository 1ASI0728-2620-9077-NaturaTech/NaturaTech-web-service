package com.plantsync.platform.analytics.domain.model.valueobjects;

public record GlobalTelemetry(
    Double averageLightPercent,
    Double averageAirTemperature,
    Double averageGasPercent,
    Double averageHumidityPercent

) {
}
