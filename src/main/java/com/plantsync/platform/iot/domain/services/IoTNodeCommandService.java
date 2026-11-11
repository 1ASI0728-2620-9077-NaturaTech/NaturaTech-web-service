package com.plantsync.platform.iot.domain.services;

import com.plantsync.platform.iot.domain.model.aggregates.IoTNode;
import com.plantsync.platform.iot.domain.model.commands.CreateNodeCommand;
import java.util.Optional;

public interface IoTNodeCommandService {

  Optional<IoTNode> handle(CreateNodeCommand command);

  Optional<IoTNode> unlinkPlant(String nodeCode);
}
