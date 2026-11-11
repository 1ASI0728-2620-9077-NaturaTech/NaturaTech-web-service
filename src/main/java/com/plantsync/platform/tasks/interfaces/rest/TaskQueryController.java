package com.plantsync.platform.tasks.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.tasks.domain.model.queries.GetAllTasksQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetPendingTasksByPlantIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTaskHistoryByPlantIdQuery;
import com.plantsync.platform.tasks.domain.model.queries.GetTasksByPlantIdQuery;
import com.plantsync.platform.tasks.domain.services.TaskQueryService;
import com.plantsync.platform.tasks.interfaces.rest.assemblers.TaskResourceFromEntityAssembler;
import com.plantsync.platform.tasks.interfaces.rest.resources.TaskResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/tasks", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Tasks", description = "Task Management Endpoints")
public class TaskQueryController {

  private final TaskQueryService taskQueryService;

  public TaskQueryController(TaskQueryService taskQueryService) {
    this.taskQueryService = taskQueryService;
  }

  @GetMapping
  @Operation(summary = "Get all tasks")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tasks found"),
    @ApiResponse(responseCode = "404", description = "No tasks found")
  })
  @PreAuthorize("hasRole('ADMIN') or (#plantId != null and @access.plant(#plantId))")
  public ResponseEntity<List<TaskResource>> getAllTasks(
      @RequestParam(required = false) Long plantId) {
    if (plantId != null) {
      var tasks = taskQueryService.handle(new GetTasksByPlantIdQuery(new PlantId(plantId)));
      if (tasks.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      var resources =
          tasks.stream().map(TaskResourceFromEntityAssembler::toResourceFromEntity).toList();
      return ResponseEntity.ok(resources);
    }
    var tasks = taskQueryService.handle(new GetAllTasksQuery());
    if (tasks.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var resources =
        tasks.stream().map(TaskResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(resources);
  }

  @GetMapping("/history")
  @Operation(summary = "Get completed task history by plant ID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "History found"),
    @ApiResponse(responseCode = "404", description = "No history found")
  })
  @PreAuthorize("@access.plant(#plantId)")
  public ResponseEntity<List<TaskResource>> getTaskHistoryByPlantId(@RequestParam Long plantId) {
    var tasks = taskQueryService.handle(new GetTaskHistoryByPlantIdQuery(new PlantId(plantId)));
    if (tasks.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var resources =
        tasks.stream().map(TaskResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(resources);
  }

  @GetMapping("/pending")
  @Operation(summary = "Get pending tasks by plant ID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pending tasks found"),
    @ApiResponse(responseCode = "404", description = "No pending tasks found")
  })
  @PreAuthorize("@access.plant(#plantId)")
  public ResponseEntity<List<TaskResource>> getPendingTasksByPlantId(@RequestParam Long plantId) {
    var tasks = taskQueryService.handle(new GetPendingTasksByPlantIdQuery(new PlantId(plantId)));
    if (tasks.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var resources =
        tasks.stream().map(TaskResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(resources);
  }
}
