package com.plantsync.platform.analytics.application.internal.queryservices;

import com.plantsync.platform.analytics.domain.model.queries.GetDeviceStatusMetricsQuery;
import com.plantsync.platform.analytics.domain.model.queries.GetGlobalTelemetryQuery;
import com.plantsync.platform.analytics.domain.model.valueobjects.DeviceStatusMetrics;
import com.plantsync.platform.analytics.domain.model.valueobjects.GlobalTelemetry;
import com.plantsync.platform.analytics.domain.services.AnalyticsQueryService;
import com.plantsync.platform.iot.domain.model.valueobjects.NodeStatus;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotSensorReadingRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

  private final IotSensorReadingRepository iotSensorReadingRepository;
  private final IotNodeRepository iotNodeRepository;

  public AnalyticsQueryServiceImpl(IotSensorReadingRepository iotSensorReadingRepository,
                                    IotNodeRepository iotNodeRepository) {
    this.iotSensorReadingRepository = iotSensorReadingRepository;
    this.iotNodeRepository = iotNodeRepository;
  }

  @Override
  public GlobalTelemetry handle(GetGlobalTelemetryQuery query) {
    var avgLightPercent = Optional.ofNullable(iotSensorReadingRepository.findAverageLightPercent())
        .orElse(0.0);
    var avgAirTemperature = Optional.ofNullable(iotSensorReadingRepository.findAverageAirTemperature())
        .orElse(0.0);
    var avgGasPercent = Optional.ofNullable(iotSensorReadingRepository.findAverageGasPercent())
        .orElse(0.0);
    var avgHumidityPercent = Optional.ofNullable(iotSensorReadingRepository.findAverageHumidityPercent())
        .orElse(0.0);
    return new GlobalTelemetry(avgLightPercent, avgAirTemperature, avgGasPercent, avgHumidityPercent);
  }

  @Override
  public DeviceStatusMetrics handle(GetDeviceStatusMetricsQuery query) {
    var onlineCount = iotNodeRepository.countByStatus(NodeStatus.ONLINE);
    var offlineCount = iotNodeRepository.countByStatus(NodeStatus.OFFLINE);
    var errorCount = iotNodeRepository.countByStatus(NodeStatus.ERROR);
    return new DeviceStatusMetrics(onlineCount, offlineCount, errorCount);
  }
}
