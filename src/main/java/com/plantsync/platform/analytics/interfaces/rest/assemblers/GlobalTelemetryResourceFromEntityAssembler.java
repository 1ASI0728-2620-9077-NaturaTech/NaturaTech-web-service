package com.plantsync.platform.analytics.interfaces.rest.assemblers;

import com.plantsync.platform.analytics.domain.model.valueobjects.GlobalTelemetry;
import com.plantsync.platform.analytics.interfaces.rest.resources.GlobalTelemetryResource;

public class GlobalTelemetryResourceFromEntityAssembler {

  public static GlobalTelemetryResource toResourceFromEntity(GlobalTelemetry entity) {
    return new GlobalTelemetryResource(
        entity.averageLightPercent(),
        entity.averageAirTemperature(),
        entity.averageGasPercent(),
        entity.averageHumidityPercent()
    );
  }
}
