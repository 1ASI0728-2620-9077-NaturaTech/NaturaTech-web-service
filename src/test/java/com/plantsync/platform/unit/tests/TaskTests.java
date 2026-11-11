package com.plantsync.platform.unit.tests;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import com.plantsync.platform.tasks.domain.model.aggregates.Task;
import com.plantsync.platform.tasks.domain.model.commands.CreateTaskCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TaskTests {

  @Test
  public void constructor_WithValidData_ShouldCreateTaskCorrectly() {
    LocalDate scheduledDate = LocalDate.of(2025, 7, 3);
    String action = "Watering";
    PlantId plantId = new PlantId(1L);
    ProfileId profileId = new ProfileId(1L);

    CreateTaskCommand command = new CreateTaskCommand(scheduledDate, action, plantId, profileId, null, null);

    Task task = new Task(command);

    assertEquals(scheduledDate, task.getScheduledDate());
    assertEquals(action, task.getAction());
    assertNull(task.getCompletedAt());
    assertEquals(plantId, task.getPlantId());
    assertEquals(profileId, task.getProfileId());
    assertNull(task.getHumidity());
    assertNull(task.getNotes());
  }

  @Test
  public void complete_ShouldSetCompletedAt() {
    LocalDate scheduledDate = LocalDate.of(2025, 7, 3);
    CreateTaskCommand command = new CreateTaskCommand(scheduledDate, "Watering", new PlantId(1L), new ProfileId(1L), null, null);
    Task task = new Task(command);

    task.complete(java.time.LocalDateTime.of(2025, 7, 3, 10, 30), 65, "Done");

    assertNotNull(task.getCompletedAt());
    assertEquals(65, task.getHumidity());
    assertEquals("Done", task.getNotes());
  }

  @Test
  public void constructor_WithNullAction_ShouldAcceptNullIfNoValidation() {
    CreateTaskCommand command = new CreateTaskCommand(
        LocalDate.now(), null, new PlantId(1L), new ProfileId(1L), null, null
    );

    Task task = new Task(command);

    assertNull(task.getAction());
  }
}
