package com.plantsync.platform.tasks.application.internal.commandservices;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import com.plantsync.platform.tasks.domain.exceptions.TaskCreationException;
import com.plantsync.platform.tasks.domain.exceptions.TaskDeletionException;
import com.plantsync.platform.tasks.domain.exceptions.TaskNotFoundException;
import com.plantsync.platform.tasks.domain.model.aggregates.Task;
import com.plantsync.platform.tasks.domain.model.commands.CompleteTaskCommand;
import com.plantsync.platform.tasks.domain.model.commands.CreateTaskCommand;
import com.plantsync.platform.tasks.domain.model.commands.DeleteTaskCommand;
import com.plantsync.platform.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskCommandServiceImplTest {

  @Mock
  private TaskRepository taskRepository;

  @InjectMocks
  private TaskCommandServiceImpl taskCommandService;

  @Test
  void handleCreateTaskCommandShouldSaveTaskAndReturnGeneratedId() {
    var command = createTaskCommand();
    var taskCaptor = ArgumentCaptor.forClass(Task.class);

    var result = taskCommandService.handle(command);

    verify(taskRepository).save(taskCaptor.capture());
    var savedTask = taskCaptor.getValue();
    assertEquals(command.scheduledDate(), savedTask.getScheduledDate());
    assertEquals(command.action(), savedTask.getAction());
    assertNull(savedTask.getCompletedAt());
    assertEquals(command.plantId(), savedTask.getPlantId());
    assertEquals(command.profileId(), savedTask.getProfileId());
    assertEquals(command.humidity(), savedTask.getHumidity());
    assertEquals(command.notes(), savedTask.getNotes());
    assertNull(result);
  }

  @Test
  void handleCreateTaskCommandShouldThrowTaskCreationExceptionWhenSaveFails() {
    var command = createTaskCommand();
    when(taskRepository.save(any(Task.class))).thenThrow(new RuntimeException("database unavailable"));

    var exception = assertThrows(TaskCreationException.class, () -> taskCommandService.handle(command));

    assertEquals("Error saving task: database unavailable", exception.getMessage());
    verify(taskRepository).save(any(Task.class));
  }

  @Test
  void handleDeleteTaskCommandShouldDeleteTaskById() {
    var command = new DeleteTaskCommand(1L);

    taskCommandService.handle(command);

    verify(taskRepository).deleteById(command.taskId());
  }

  @Test
  void handleDeleteTaskCommandShouldThrowTaskDeletionExceptionWhenDeleteFails() {
    var command = new DeleteTaskCommand(1L);
    doThrow(new RuntimeException("delete failed")).when(taskRepository).deleteById(command.taskId());

    var exception = assertThrows(TaskDeletionException.class, () -> taskCommandService.handle(command));

    assertEquals("Error while deleting task: delete failed", exception.getMessage());
    verify(taskRepository).deleteById(command.taskId());
  }

  @Test
  void handleCompleteTaskCommandShouldMarkTaskAsCompleted() {
    var task = new Task(createTaskCommand());
    when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

    var command = new CompleteTaskCommand(1L, 65, "Done");
    taskCommandService.handle(command);

    assertNotNull(task.getCompletedAt());
    assertEquals(65, task.getHumidity());
    assertEquals("Done", task.getNotes());
    verify(taskRepository).save(task);
  }

  @Test
  void handleCompleteTaskCommandShouldThrowWhenTaskNotFound() {
    when(taskRepository.findById(99L)).thenReturn(Optional.empty());

    var command = new CompleteTaskCommand(99L, null, null);

    assertThrows(TaskNotFoundException.class, () -> taskCommandService.handle(command));
  }

  private CreateTaskCommand createTaskCommand() {
    return new CreateTaskCommand(
        LocalDate.of(2026, 1, 17),
        "Water plant",
        new PlantId(1L),
        new ProfileId(1L),
        null,
        null
    );
  }
}
