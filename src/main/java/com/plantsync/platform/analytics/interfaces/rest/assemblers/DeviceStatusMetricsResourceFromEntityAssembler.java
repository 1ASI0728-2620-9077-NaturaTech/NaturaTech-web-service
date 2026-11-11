package com.plantsync.platform.analytics.interfaces.rest.assemblers;

import com.plantsync.platform.analytics.domain.model.valueobjects.DeviceStatusMetrics;
import com.plantsync.platform.analytics.interfaces.rest.resources.DeviceStatusMetricsResource;

public class DeviceStatusMetricsResourceFromEntityAssembler {

  public static DeviceStatusMetricsResource toResourceFromEntity(DeviceStatusMetrics entity) {
    return new DeviceStatusMetricsResource(
        entity.onlineCount(),
        entity.offlineCount(),
        entity.errorCount()
    );
  }
}
