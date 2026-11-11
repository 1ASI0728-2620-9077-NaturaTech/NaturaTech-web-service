package com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories;

import com.plantsync.platform.iot.domain.model.aggregates.IoTNode;
import com.plantsync.platform.iot.domain.model.valueobjects.NodeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IotNodeRepository extends JpaRepository<IoTNode, Long> {

  Optional<IoTNode> findByNodeCode(String nodeCode);

  Optional<IoTNode> findByPlantIdValue(Long plantId);

  List<IoTNode> findByProfileIdValue(Long profileId);

  long countByStatus(NodeStatus status);
}
