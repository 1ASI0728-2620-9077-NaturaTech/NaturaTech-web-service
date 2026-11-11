package com.plantsync.platform.tasks.application.internal.queryservices;

import com.plantsync.platform.tasks.domain.model.aggregates.Task;
import com.plantsync.platform.tasks.domain.model.queries.GetAllTasksQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetPendingTasksByPlantIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTaskByIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTaskHistoryByPlantIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTasksByPlantIdQuery;
import com.plantsync.platform.tasks.domain.services.TaskQueryService;
import com.plantsync.platform.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TaskQueryServiceImpl implements TaskQueryService {

  private final TaskRepository taskRepository;

  public TaskQueryServiceImpl(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Override
  public List<Task> handle(GetAllTasksQuery query) {
    return taskRepository.findAll();
  }

  @Override
  public Optional<Task> handle(GetTaskByIdQuery query) {
    return taskRepository.findById(query.taskId());
  }

  @Override
  public List<Task> handle(GetTaskHistoryByPlantIdQuery query) {
    return taskRepository.findByPlantIdAndCompletedAtIsNotNull(query.plantId());
  }

  @Override
  public List<Task> handle(GetPendingTasksByPlantIdQuery query) {
    return taskRepository.findByPlantIdAndCompletedAtIsNull(query.plantId());
  }

  @Override
  public List<Task> handle(GetTasksByPlantIdQuery query) {
    return taskRepository.findByPlantId(query.plantId());
  }
}
