package com.plantsync.platform.tasks.domain.services;

import com.plantsync.platform.tasks.domain.model.commands.CompleteTaskCommand;
import com.plantsync.platform.tasks.domain.model.commands.CreateTaskCommand;
import com.plantsync.platform.tasks.domain.model.commands.DeleteTaskCommand;

public interface TaskCommandService {

  Long handle(CreateTaskCommand command);

  void handle(DeleteTaskCommand command);

  void handle(CompleteTaskCommand command);

}
