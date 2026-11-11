package com.plantsync.platform.iot.domain.services;

import com.plantsync.platform.iot.domain.model.aggregates.IoTNode;
import com.plantsync.platform.iot.domain.model.queries.GetAllNodesByProfileIdQuery;
import com.plantsync.platform.iot.domain.model.queries.GetNodeByIdQuery;
import com.plantsync.platform.iot.domain.model.queries.GetNodeByNodeCodeQuery;
import java.util.List;
import java.util.Optional;

public interface IoTNodeQueryService {

  Optional<IoTNode> handle(GetNodeByIdQuery query);

  Optional<IoTNode> handle(GetNodeByNodeCodeQuery query);

  List<IoTNode> handle(GetAllNodesByProfileIdQuery query);
}
