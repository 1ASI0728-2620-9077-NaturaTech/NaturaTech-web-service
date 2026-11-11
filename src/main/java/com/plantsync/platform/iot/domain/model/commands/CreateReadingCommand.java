package com.plantsync.platform.iot.domain.model.commands;

public record CreateReadingCommand(Long nodeId, Integer lightPercent, Integer gasPercent, Float airTemperature, Integer humidityPercent) {
}
