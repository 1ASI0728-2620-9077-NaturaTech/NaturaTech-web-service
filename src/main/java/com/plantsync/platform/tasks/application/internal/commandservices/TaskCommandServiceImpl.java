package com.plantsync.platform.tasks.application.internal.commandservices;

import com.plantsync.platform.tasks.domain.exceptions.TaskCreationException;
import com.plantsync.platform.tasks.domain.exceptions.TaskDeletionException;
import com.plantsync.platform.tasks.domain.exceptions.TaskNotFoundException;
import com.plantsync.platform.tasks.domain.model.aggregates.Task;
import com.plantsync.platform.tasks.domain.model.commands.CompleteTaskCommand;
import com.plantsync.platform.tasks.domain.model.commands.CreateTaskCommand;
import com.plantsync.platform.tasks.domain.model.commands.DeleteTaskCommand;
import com.plantsync.platform.tasks.domain.services.TaskCommandService;
import com.plantsync.platform.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class TaskCommandServiceImpl implements TaskCommandService {

  private final TaskRepository taskRepository;

  public TaskCommandServiceImpl(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Override
  public Long handle(CreateTaskCommand command) {
    var task = new Task(command);
    try {
      taskRepository.save(task);
    } catch (Exception e) {
      throw new TaskCreationException(e.getMessage());
    }
    return task.getId();
  }

  @Override
  public void handle(DeleteTaskCommand command) {
    try {
      taskRepository.deleteById(command.taskId());
    } catch (Exception e) {
      throw new TaskDeletionException(e.getMessage());
    }
  }

  @Override
  public void handle(CompleteTaskCommand command) {
    var task = taskRepository.findById(command.taskId())
        .orElseThrow(() -> new TaskNotFoundException(command.taskId()));
    task.complete(LocalDateTime.now(), command.humidity(), command.notes());
    taskRepository.save(task);
  }
}
