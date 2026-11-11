package com.plantsync.platform.iot.domain.model.entities;

import com.plantsync.platform.iot.domain.model.commands.CreateReadingCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sensor_readings")
public class SensorReading {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "node_id", nullable = false)
  private Long nodeId;

  @Column(name = "light_percent")
  private Integer lightPercent;

  @Column(name = "gas_percent")
  private Integer gasPercent;

  @Column(name = "air_temperature")
  private Float airTemperature;

  @Column(name = "humidity_percent")
  private Integer humidityPercent;

  @Column(name = "timestamp", nullable = false)
  private LocalDateTime timestamp;

  public SensorReading(CreateReadingCommand command) {
    this.nodeId = command.nodeId();
    this.lightPercent = command.lightPercent();
    this.gasPercent = command.gasPercent();
    this.airTemperature = command.airTemperature();
    this.humidityPercent = command.humidityPercent();
    this.timestamp = LocalDateTime.now();
  }
}
