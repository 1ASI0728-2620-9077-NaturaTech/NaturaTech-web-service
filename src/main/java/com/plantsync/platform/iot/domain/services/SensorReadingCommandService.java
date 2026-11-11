package com.plantsync.platform.iot.domain.services;

import com.plantsync.platform.iot.domain.model.entities.SensorReading;
import com.plantsync.platform.iot.domain.model.commands.CreateReadingCommand;

public interface SensorReadingCommandService {

  SensorReading handle(CreateReadingCommand command);
}
