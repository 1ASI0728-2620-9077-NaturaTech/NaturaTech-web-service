package com.plantsync.platform.analytics.interfaces.rest.resources;

public record DeviceStatusMetricsResource(
    Long onlineCount,
    Long offlineCount,
    Long errorCount
) {
}
