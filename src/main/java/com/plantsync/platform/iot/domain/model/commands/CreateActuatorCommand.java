package com.plantsync.platform.iot.domain.model.commands;

import com.plantsync.platform.iot.domain.model.valueobjects.ActuatorAction;
import com.plantsync.platform.iot.domain.model.valueobjects.ActuatorType;

public record CreateActuatorCommand(Long nodeId, ActuatorType actuatorType, ActuatorAction action) {
}
