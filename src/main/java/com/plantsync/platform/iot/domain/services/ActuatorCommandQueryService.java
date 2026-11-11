package com.plantsync.platform.iot.domain.services;

import com.plantsync.platform.iot.domain.model.entities.ActuatorCommand;
import com.plantsync.platform.iot.domain.model.queries.GetPendingActuatorCommandsByNodeIdQuery;
import java.util.List;

public interface ActuatorCommandQueryService {

  List<ActuatorCommand> handle(GetPendingActuatorCommandsByNodeIdQuery query);
}
