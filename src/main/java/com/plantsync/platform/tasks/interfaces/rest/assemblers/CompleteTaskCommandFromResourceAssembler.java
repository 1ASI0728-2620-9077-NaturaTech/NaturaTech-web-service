package com.plantsync.platform.tasks.interfaces.rest.assemblers;

import com.plantsync.platform.tasks.domain.model.commands.CompleteTaskCommand;
import com.plantsync.platform.tasks.interfaces.rest.resources.CompleteTaskResource;

public record CompleteTaskCommandFromResourceAssembler() {

  public static CompleteTaskCommand toCommandFromResource(Long taskId, CompleteTaskResource resource) {
    return new CompleteTaskCommand(
        taskId,
        resource.humidity(),
        resource.notes()
    );
  }

}
