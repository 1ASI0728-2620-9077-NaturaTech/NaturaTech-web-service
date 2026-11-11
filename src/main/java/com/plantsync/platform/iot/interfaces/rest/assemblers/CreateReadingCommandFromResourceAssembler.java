package com.plantsync.platform.iot.interfaces.rest.assemblers;

import com.plantsync.platform.iot.domain.model.commands.CreateReadingCommand;
import com.plantsync.platform.iot.interfaces.rest.resources.CreateReadingResource;

public class CreateReadingCommandFromResourceAssembler {

  public static CreateReadingCommand toCommandFromResource(CreateReadingResource resource) {
    return new CreateReadingCommand(
        resource.nodeId(),
        resource.lightPercent(),
        resource.gasPercent(),
        resource.airTemperature(),
        resource.humidityPercent()
    );
  }
}
