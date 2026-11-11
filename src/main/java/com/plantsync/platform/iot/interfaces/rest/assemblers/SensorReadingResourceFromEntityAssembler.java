package com.plantsync.platform.iot.interfaces.rest.assemblers;

import com.plantsync.platform.iot.domain.model.entities.SensorReading;
import com.plantsync.platform.iot.interfaces.rest.resources.SensorReadingResource;

public class SensorReadingResourceFromEntityAssembler {

  public static SensorReadingResource toResourceFromEntity(SensorReading entity) {
    return new SensorReadingResource(
        entity.getId(),
        entity.getNodeId(),
        entity.getLightPercent(),
        entity.getGasPercent(),
        entity.getAirTemperature(),
        entity.getHumidityPercent(),
        entity.getTimestamp().toString()
    );
  }
}
