package com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories;

import com.plantsync.platform.iot.domain.model.entities.ActuatorCommand;
import com.plantsync.platform.iot.domain.model.valueobjects.CommandStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActuatorCommandRepository extends JpaRepository<ActuatorCommand, Long> {

  List<ActuatorCommand> findByNodeIdAndStatus(Long nodeId, CommandStatus status);
}
