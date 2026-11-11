package com.plantsync.platform.iot.application.internal.commandservices;

import com.plantsync.platform.iot.domain.model.aggregates.IoTNode;
import com.plantsync.platform.iot.domain.model.commands.CreateNodeCommand;
import com.plantsync.platform.iot.domain.services.IoTNodeCommandService;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository;
import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IoTNodeCommandServiceImpl implements IoTNodeCommandService {

  private final IotNodeRepository iotNodeRepository;

  public IoTNodeCommandServiceImpl(IotNodeRepository iotNodeRepository) {
    this.iotNodeRepository = iotNodeRepository;
  }

  @Override
  public Optional<IoTNode> handle(CreateNodeCommand command) {
    if (iotNodeRepository.findByNodeCode(command.nodeCode()).isPresent()) {
      return Optional.empty();
    }
    if (command.plantId() != null
        && iotNodeRepository.findByPlantIdValue(command.plantId().value()).isPresent()) {
      return Optional.empty();
    }
    var node = new IoTNode(command);
    var savedNode = iotNodeRepository.save(node);
    return Optional.of(savedNode);
  }

  @Transactional
  public Optional<IoTNode> linkPlant(String nodeCode, Long plantId) {
    var nodeOpt = iotNodeRepository.findByNodeCode(nodeCode);
    if (nodeOpt.isEmpty()) {
      return Optional.empty();
    }
    if (iotNodeRepository.findByPlantIdValue(plantId).isPresent()) {
      throw new IllegalStateException(
          "Plant with id " + plantId + " is already linked to another node");
    }
    var node = nodeOpt.get();
    node.setPlantId(new PlantId(plantId));
    node.setLinkedAt(java.time.Instant.now());
    return Optional.of(iotNodeRepository.save(node));
  }

  @Transactional
  @Override
  public Optional<IoTNode> unlinkPlant(String nodeCode) {
    var nodeOpt = iotNodeRepository.findByNodeCode(nodeCode);
    if (nodeOpt.isEmpty()) {
      return Optional.empty();
    }
    var node = nodeOpt.get();
    node.setPlantId(null);
    node.setLinkedAt(java.time.Instant.now());
    return Optional.of(iotNodeRepository.save(node));
  }
}
