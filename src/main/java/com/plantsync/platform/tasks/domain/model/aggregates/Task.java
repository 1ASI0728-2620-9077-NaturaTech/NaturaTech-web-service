package com.plantsync.platform.tasks.domain.model.aggregates;

import com.plantsync.platform.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import com.plantsync.platform.tasks.domain.model.commands.CreateTaskCommand;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Task aggregate root.
 * Represents a task associated with a plant and a profile.
 */
@Getter
@Setter
@Entity
public class Task extends AuditableAbstractAggregateRoot<Task> {

  @NotNull
  private LocalDate scheduledDate;

  @NotBlank
  private String action;

  private LocalDateTime completedAt;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "plant_id"))
  private PlantId plantId;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "profile_id"))
  private ProfileId profileId;

  private Integer humidity;

  private String notes;

  /**
   * Default constructor for Task.
   */
  public Task() {
  }

  /**
   * Constructor for Task from CreateTaskCommand.
   *
   * @param command The {@link CreateTaskCommand} instance.
   */
  public Task(CreateTaskCommand command) {
    this.scheduledDate = command.scheduledDate();
    this.action = command.action();
    this.completedAt = null;
    this.plantId = command.plantId();
    this.profileId = command.profileId();
    this.humidity = command.humidity();
    this.notes = command.notes();
  }

  /**
   * Marks the task as completed.
   *
   * @param completedAt The timestamp of completion.
   * @param humidity    Optional humidity reading.
   * @param notes       Optional completion notes.
   */
  public void complete(LocalDateTime completedAt, Integer humidity, String notes) {
    this.completedAt = completedAt;
    this.humidity = humidity;
    this.notes = notes;
  }

  /**
   * Updates the task information.
   *
   * @param newAction       The new action description.
   * @param newScheduledDate The new scheduled date for the task.
   * @param newPlantId      The new {@link PlantId}.
   * @param newProfileId    The new {@link ProfileId}.
   * @return The updated {@link Task} instance.
   */
  public Task updateInformation(
      String newAction,
      LocalDate newScheduledDate,
      PlantId newPlantId,
      ProfileId newProfileId
  ) {
    this.action = newAction;
    this.scheduledDate = newScheduledDate;
    this.plantId = newPlantId;
    this.profileId = newProfileId;

    return this;
  }

}