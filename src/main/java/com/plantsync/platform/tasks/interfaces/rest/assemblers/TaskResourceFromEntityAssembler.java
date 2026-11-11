package com.plantsync.platform.tasks.interfaces.rest.assemblers;

import com.plantsync.platform.tasks.domain.model.aggregates.Task;
import com.plantsync.platform.tasks.interfaces.rest.resources.TaskResource;

public class TaskResourceFromEntityAssembler {

  public static TaskResource toResourceFromEntity(Task entity) {
    return new TaskResource(
        entity.getId(),
        entity.getAction(),
        entity.getScheduledDate().toString(),
        entity.getCompletedAt() != null ? entity.getCompletedAt().toString() : null,
        entity.getPlantId().value(),
        entity.getProfileId().value(),
        entity.getHumidity(),
        entity.getNotes(),
        entity.getCompletedAt() != null ? "COMPLETED" : "PENDING"
    );
  }

}
