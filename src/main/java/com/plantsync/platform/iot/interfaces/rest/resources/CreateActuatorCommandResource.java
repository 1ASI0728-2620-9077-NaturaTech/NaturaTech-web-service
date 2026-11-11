package com.plantsync.platform.iot.interfaces.rest.resources;

import com.plantsync.platform.iot.domain.model.valueobjects.ActuatorAction;
import com.plantsync.platform.iot.domain.model.valueobjects.ActuatorType;
import jakarta.validation.constraints.NotNull;

public record CreateActuatorCommandResource(
    @NotNull String nodeCode,
    @NotNull ActuatorType actuatorType,
    @NotNull ActuatorAction action
) {
}
