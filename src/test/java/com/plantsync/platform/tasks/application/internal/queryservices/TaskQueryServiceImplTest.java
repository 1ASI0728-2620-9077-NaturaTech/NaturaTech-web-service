package com.plantsync.platform.tasks.application.internal.queryservices;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import com.plantsync.platform.tasks.domain.model.aggregates.Task;
import com.plantsync.platform.tasks.domain.model.commands.CreateTaskCommand;
import com.plantsync.platform.tasks.domain.model.queries.GetAllTasksQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetPendingTasksByPlantIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTaskByIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTaskHistoryByPlantIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTasksByPlantIdQuery;
import com.plantsync.platform.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskQueryServiceImplTest {

  @Mock
  private TaskRepository taskRepository;

  @InjectMocks
  private TaskQueryServiceImpl taskQueryService;

  @Test
  void handleGetAllTasksQueryShouldReturnAllTasks() {
    var query = new GetAllTasksQuery();
    var tasks = List.of(createTask());
    when(taskRepository.findAll()).thenReturn(tasks);

    var result = taskQueryService.handle(query);

    assertEquals(tasks, result);
    verify(taskRepository).findAll();
  }

  @Test
  void handleGetTaskByIdQueryShouldReturnTaskWhenItExists() {
    var query = new GetTaskByIdQuery(1L);
    var task = createTask();
    when(taskRepository.findById(query.taskId())).thenReturn(Optional.of(task));

    var result = taskQueryService.handle(query);

    assertTrue(result.isPresent());
    assertSame(task, result.get());
    verify(taskRepository).findById(query.taskId());
  }

  @Test
  void handleGetTaskHistoryByPlantIdQueryShouldReturnCompletedTasks() {
    var plantId = new PlantId(1L);
    var query = new GetTaskHistoryByPlantIdQuery(plantId);
    var tasks = List.of(createTask());
    when(taskRepository.findByPlantIdAndCompletedAtIsNotNull(plantId)).thenReturn(tasks);

    var result = taskQueryService.handle(query);

    assertEquals(tasks, result);
    verify(taskRepository).findByPlantIdAndCompletedAtIsNotNull(plantId);
  }

  @Test
  void handleGetPendingTasksByPlantIdQueryShouldReturnPendingTasks() {
    var plantId = new PlantId(1L);
    var query = new GetPendingTasksByPlantIdQuery(plantId);
    var tasks = List.of(createTask());
    when(taskRepository.findByPlantIdAndCompletedAtIsNull(plantId)).thenReturn(tasks);

    var result = taskQueryService.handle(query);

    assertEquals(tasks, result);
    verify(taskRepository).findByPlantIdAndCompletedAtIsNull(plantId);
  }

  @Test
  void handleGetTasksByPlantIdQueryShouldReturnAllTasksForPlant() {
    var plantId = new PlantId(1L);
    var query = new GetTasksByPlantIdQuery(plantId);
    var tasks = List.of(createTask());
    when(taskRepository.findByPlantId(plantId)).thenReturn(tasks);

    var result = taskQueryService.handle(query);

    assertEquals(tasks, result);
    verify(taskRepository).findByPlantId(plantId);
  }

  private Task createTask() {
    return new Task(new CreateTaskCommand(
        LocalDate.of(2026, 1, 17),
        "Water plant",
        new PlantId(1L),
        new ProfileId(1L),
        null,
        null
    ));
  }
}
