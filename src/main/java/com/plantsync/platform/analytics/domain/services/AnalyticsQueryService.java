package com.plantsync.platform.analytics.domain.services;

import com.plantsync.platform.analytics.domain.model.queries.GetDeviceStatusMetricsQuery;
import com.plantsync.platform.analytics.domain.model.queries.GetGlobalTelemetryQuery;
import com.plantsync.platform.analytics.domain.model.valueobjects.DeviceStatusMetrics;
import com.plantsync.platform.analytics.domain.model.valueobjects.GlobalTelemetry;

public interface AnalyticsQueryService {

  GlobalTelemetry handle(GetGlobalTelemetryQuery query);

  DeviceStatusMetrics handle(GetDeviceStatusMetricsQuery query);
}
