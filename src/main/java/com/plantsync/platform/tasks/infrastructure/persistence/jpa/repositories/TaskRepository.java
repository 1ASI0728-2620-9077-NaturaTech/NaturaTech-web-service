package com.plantsync.platform.tasks.infrastructure.persistence.jpa.repositories;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.tasks.domain.model.aggregates.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

  List<Task> findByPlantId(PlantId plantId);

  List<Task> findByPlantIdAndCompletedAtIsNotNull(PlantId plantId);

  List<Task> findByPlantIdAndCompletedAtIsNull(PlantId plantId);
}
