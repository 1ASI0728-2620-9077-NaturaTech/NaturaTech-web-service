package com.plantsync.platform.iot.domain.services;

import com.plantsync.platform.iot.domain.model.entities.ActuatorCommand;
import com.plantsync.platform.iot.domain.model.commands.CompleteActuatorCommand;
import com.plantsync.platform.iot.domain.model.commands.CreateActuatorCommand;

public interface ActuatorCommandService {

  ActuatorCommand handle(CreateActuatorCommand command);

  ActuatorCommand handle(CompleteActuatorCommand command);
}
