package com.plantsync.platform.iot.domain.model.entities;

import com.plantsync.platform.iot.domain.model.commands.CreateActuatorCommand;
import com.plantsync.platform.iot.domain.model.valueobjects.ActuatorAction;
import com.plantsync.platform.iot.domain.model.valueobjects.ActuatorType;
import com.plantsync.platform.iot.domain.model.valueobjects.CommandStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "actuator_commands")
public class ActuatorCommand {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "node_id", nullable = false)
  private Long nodeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "actuator_type", nullable = false, length = 30)
  private ActuatorType actuatorType;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 20)
  private ActuatorAction action;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CommandStatus status;

  @Column(name = "issued_at", nullable = false)
  private LocalDateTime issuedAt;

  public ActuatorCommand(CreateActuatorCommand command) {
    this.nodeId = command.nodeId();
    this.actuatorType = command.actuatorType();
    this.action = command.action();
    this.status = CommandStatus.PENDING;
    this.issuedAt = LocalDateTime.now();
  }

  public void markAsExecuted() {
    this.status = CommandStatus.EXECUTED;
  }
}
