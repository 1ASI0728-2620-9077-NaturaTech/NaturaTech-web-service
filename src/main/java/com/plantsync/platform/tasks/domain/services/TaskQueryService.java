package com.plantsync.platform.tasks.domain.services;

import com.plantsync.platform.tasks.domain.model.aggregates.Task;
import com.plantsync.platform.tasks.domain.model.queries.GetAllTasksQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetPendingTasksByPlantIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTaskByIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTaskHistoryByPlantIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTasksByPlantIdQuery;
import java.util.List;
import java.util.Optional;

public interface TaskQueryService {

  List<Task> handle(GetAllTasksQuery query);

  Optional<Task> handle(GetTaskByIdQuery query);

  List<Task> handle(GetTaskHistoryByPlantIdQuery query);

  List<Task> handle(GetPendingTasksByPlantIdQuery query);

  List<Task> handle(GetTasksByPlantIdQuery query);

}
