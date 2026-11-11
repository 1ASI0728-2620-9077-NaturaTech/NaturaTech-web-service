package com.plantsync.platform.iot.interfaces.rest.assemblers;

import com.plantsync.platform.iot.domain.model.entities.ActuatorCommand;
import com.plantsync.platform.iot.interfaces.rest.resources.ActuatorCommandResource;

public class ActuatorCommandResourceFromEntityAssembler {

  public static ActuatorCommandResource toResourceFromEntity(ActuatorCommand entity) {
    return new ActuatorCommandResource(
        entity.getId(),
        entity.getNodeId(),
        entity.getActuatorType().name(),
        entity.getAction().name(),
        entity.getStatus().name(),
        entity.getIssuedAt().toString()
    );
  }
}
