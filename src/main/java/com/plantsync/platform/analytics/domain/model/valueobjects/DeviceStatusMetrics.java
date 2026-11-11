package com.plantsync.platform.analytics.domain.model.valueobjects;

public record DeviceStatusMetrics(
    Long onlineCount,
    Long offlineCount,
    Long errorCount
) {
}
