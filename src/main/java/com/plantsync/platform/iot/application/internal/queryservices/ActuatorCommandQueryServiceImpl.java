package com.plantsync.platform.iot.application.internal.queryservices;

import com.plantsync.platform.iot.domain.model.entities.ActuatorCommand;
import com.plantsync.platform.iot.domain.model.queries.GetPendingActuatorCommandsByNodeIdQuery;
import com.plantsync.platform.iot.domain.model.valueobjects.CommandStatus;
import com.plantsync.platform.iot.domain.services.ActuatorCommandQueryService;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.ActuatorCommandRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ActuatorCommandQueryServiceImpl implements ActuatorCommandQueryService {

  private final ActuatorCommandRepository actuatorCommandRepository;

  public ActuatorCommandQueryServiceImpl(ActuatorCommandRepository actuatorCommandRepository) {
    this.actuatorCommandRepository = actuatorCommandRepository;
  }

  @Override
  public List<ActuatorCommand> handle(GetPendingActuatorCommandsByNodeIdQuery query) {
    return actuatorCommandRepository.findByNodeIdAndStatus(query.nodeId(), CommandStatus.PENDING);
  }
}
