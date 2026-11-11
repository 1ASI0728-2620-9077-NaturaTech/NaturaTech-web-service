package com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories;

import com.plantsync.platform.iot.domain.model.entities.SensorReading;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IotSensorReadingRepository extends JpaRepository<SensorReading, Long> {

  List<SensorReading> findByNodeIdAndTimestampBetween(Long nodeId, LocalDateTime start, LocalDateTime end);

  Optional<SensorReading> findTopByNodeIdOrderByTimestampDesc(Long nodeId);

  @Query("SELECT AVG(s.lightPercent) FROM SensorReading s")
  Double findAverageLightPercent();

  @Query("SELECT AVG(s.airTemperature) FROM SensorReading s")
  Double findAverageAirTemperature();

  @Query("SELECT AVG(s.gasPercent) FROM SensorReading s")
  Double findAverageGasPercent();

  @Query("SELECT AVG(s.humidityPercent) FROM SensorReading s")
  Double findAverageHumidityPercent();
}
