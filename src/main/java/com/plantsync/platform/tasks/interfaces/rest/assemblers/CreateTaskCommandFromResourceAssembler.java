package com.plantsync.platform.tasks.interfaces.rest.assemblers;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import com.plantsync.platform.tasks.domain.model.commands.CreateTaskCommand;
import com.plantsync.platform.tasks.interfaces.rest.resources.CreateTaskResource;
import java.time.LocalDate;

public record CreateTaskCommandFromResourceAssembler() {

  public static CreateTaskCommand toCommandFromResource(CreateTaskResource resource) {
    return new CreateTaskCommand(
        LocalDate.parse(resource.scheduledDate()),
        resource.action(),
        new PlantId(resource.plantId()),
        new ProfileId(resource.profileId()),
        resource.humidity(),
        resource.notes()
    );
  }

}
