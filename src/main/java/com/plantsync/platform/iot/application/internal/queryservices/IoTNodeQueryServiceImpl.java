package com.plantsync.platform.iot.application.internal.queryservices;

import com.plantsync.platform.iot.domain.model.aggregates.IoTNode;
import com.plantsync.platform.iot.domain.model.queries.GetAllNodesByProfileIdQuery;
import com.plantsync.platform.iot.domain.model.queries.GetNodeByIdQuery;
import com.plantsync.platform.iot.domain.model.queries.GetNodeByNodeCodeQuery;
import com.plantsync.platform.iot.domain.services.IoTNodeQueryService;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IoTNodeQueryServiceImpl implements IoTNodeQueryService {

  private final IotNodeRepository iotNodeRepository;

  public IoTNodeQueryServiceImpl(IotNodeRepository iotNodeRepository) {
    this.iotNodeRepository = iotNodeRepository;
  }

  @Override
  public Optional<IoTNode> handle(GetNodeByIdQuery query) {
    return iotNodeRepository.findById(query.id());
  }

  @Override
  public Optional<IoTNode> handle(GetNodeByNodeCodeQuery query) {
    return iotNodeRepository.findByNodeCode(query.nodeCode());
  }

  @Override
  public List<IoTNode> handle(GetAllNodesByProfileIdQuery query) {
    return iotNodeRepository.findByProfileIdValue(query.profileId().value());
  }
}
