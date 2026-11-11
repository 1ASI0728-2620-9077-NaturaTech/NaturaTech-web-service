package com.plantsync.platform.tasks.domain.model.commands;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import java.time.LocalDate;

/**
 * The type Create task command.
 */
public record CreateTaskCommand(

    LocalDate scheduledDate,
    String action,
    PlantId plantId,
    ProfileId profileId,
    Integer humidity,
    String notes

) {

  /**
   * Instantiates a new Create task command.
   *
   * @param scheduledDate the scheduled date
   * @param action        the action
   * @param plantId       the plant id
   * @param profileId     the profile id
   * @param humidity      the humidity
   * @param notes         the notes
   */
  public CreateTaskCommand {

  }
}