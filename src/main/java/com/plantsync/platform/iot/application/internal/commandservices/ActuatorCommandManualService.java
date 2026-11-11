package com.plantsync.platform.iot.application.internal.commandservices;

import com.plantsync.platform.iot.domain.model.commands.CreateActuatorCommand;
import com.plantsync.platform.iot.domain.model.entities.ActuatorCommand;
import com.plantsync.platform.iot.domain.model.queries.GetNodeByNodeCodeQuery;
import com.plantsync.platform.iot.domain.model.valueobjects.ActuatorAction;
import com.plantsync.platform.iot.domain.model.valueobjects.ActuatorType;
import com.plantsync.platform.iot.domain.model.valueobjects.BuzzerMode;
import com.plantsync.platform.iot.domain.model.valueobjects.DesiredActuatorState;
import com.plantsync.platform.iot.domain.model.valueobjects.LedMode;
import com.plantsync.platform.iot.domain.model.valueobjects.ServoMode;
import com.plantsync.platform.iot.domain.services.ActuatorCommandService;
import com.plantsync.platform.iot.domain.services.IoTNodeQueryService;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository;
import org.springframework.stereotype.Service;

@Service
public class ActuatorCommandManualService {

  private final IoTNodeQueryService iotNodeQueryService;
  private final ActuatorCommandService actuatorCommandService;
  private final IotNodeRepository iotNodeRepository;

  public ActuatorCommandManualService(IoTNodeQueryService iotNodeQueryService,
                                       ActuatorCommandService actuatorCommandService,
                                       IotNodeRepository iotNodeRepository) {
    this.iotNodeQueryService = iotNodeQueryService;
    this.actuatorCommandService = actuatorCommandService;
    this.iotNodeRepository = iotNodeRepository;
  }

  public ActuatorCommand handle(String nodeCode, ActuatorType actuatorType, ActuatorAction action) {
    var nodeOpt = iotNodeQueryService.handle(new GetNodeByNodeCodeQuery(nodeCode));
    if (nodeOpt.isEmpty()) {
      throw new IllegalArgumentException("Node not found: " + nodeCode);
    }
    var node = nodeOpt.get();
    var command = new CreateActuatorCommand(node.getId(), actuatorType, action);
    var result = actuatorCommandService.handle(command);

    var desired = node.getDesiredState();
    var buzzerMode = desired.buzzerMode();
    var servoMode = desired.servoMode();
    var ledMode = desired.ledMode();

    switch (actuatorType) {
      case BUZZER -> buzzerMode = action == ActuatorAction.ACTIVATE ? BuzzerMode.ON : BuzzerMode.OFF;
      case WATER_SPRAYER, SERVO -> servoMode = action == ActuatorAction.ACTIVATE ? ServoMode.ON : ServoMode.OFF;
      case UV_LIGHT, LED -> ledMode = action == ActuatorAction.ACTIVATE ? LedMode.ON : LedMode.OFF;
    }

    node.setDesiredState(new DesiredActuatorState(buzzerMode, servoMode, ledMode));
    iotNodeRepository.save(node);

    return result;
  }
}
