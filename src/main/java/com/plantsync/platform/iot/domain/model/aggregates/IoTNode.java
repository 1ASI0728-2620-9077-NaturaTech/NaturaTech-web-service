package com.plantsync.platform.iot.domain.model.aggregates;

import com.plantsync.platform.iot.domain.model.commands.CreateNodeCommand;
import com.plantsync.platform.iot.domain.model.valueobjects.BuzzerMode;
import com.plantsync.platform.iot.domain.model.valueobjects.DesiredActuatorState;
import com.plantsync.platform.iot.domain.model.valueobjects.LedMode;
import com.plantsync.platform.iot.domain.model.valueobjects.NodeStatus;
import com.plantsync.platform.iot.domain.model.valueobjects.ReportedActuatorState;
import com.plantsync.platform.iot.domain.model.valueobjects.ServoMode;
import com.plantsync.platform.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "iot_nodes")
public class IoTNode extends AuditableAbstractAggregateRoot<IoTNode> {

  private java.time.Instant linkedAt;

  @Column(name = "node_code", nullable = false, unique = true)
  private String nodeCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private NodeStatus status;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "plant_id"))
  private PlantId plantId;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "profile_id"))
  private ProfileId profileId;

  @Embedded private DesiredActuatorState desiredState;

  @Embedded private ReportedActuatorState reportedState;

  public IoTNode() {
    this.desiredState = new DesiredActuatorState();
    this.reportedState = new ReportedActuatorState(BuzzerMode.OFF, ServoMode.OFF, LedMode.OFF);
  }

  public IoTNode(CreateNodeCommand command) {
    this();
    this.nodeCode = command.nodeCode();
    this.status = NodeStatus.ONLINE;
    if (command.plantId() != null) {
      this.plantId = command.plantId();
    }
    if (command.profileId() != null) {
      this.profileId = command.profileId();
    }
  }
}
