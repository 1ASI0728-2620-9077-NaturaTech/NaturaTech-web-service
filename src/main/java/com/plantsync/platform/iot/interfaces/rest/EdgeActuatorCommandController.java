package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.domain.model.commands.CompleteActuatorCommand;
import com.plantsync.platform.iot.domain.model.queries.GetNodeByNodeCodeQuery;
import com.plantsync.platform.iot.domain.model.queries.GetPendingActuatorCommandsByNodeIdQuery;
import com.plantsync.platform.iot.domain.services.ActuatorCommandQueryService;
import com.plantsync.platform.iot.domain.services.ActuatorCommandService;
import com.plantsync.platform.iot.domain.services.IoTNodeQueryService;
import com.plantsync.platform.iot.interfaces.rest.assemblers.ActuatorCommandResourceFromEntityAssembler;
import com.plantsync.platform.iot.interfaces.rest.resources.ActuatorCommandResource;
import com.plantsync.platform.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/api/v1/iot/edge/nodes/{nodeCode}/actuator-commands",
    produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Edge Actuator Commands", description = "Edge Service actuator polling endpoints")
public class EdgeActuatorCommandController {

  private final ActuatorCommandQueryService actuatorCommandQueryService;
  private final ActuatorCommandService actuatorCommandService;
  private final IoTNodeQueryService iotNodeQueryService;

  public EdgeActuatorCommandController(ActuatorCommandQueryService actuatorCommandQueryService,
                                       ActuatorCommandService actuatorCommandService,
                                       IoTNodeQueryService iotNodeQueryService) {
    this.actuatorCommandQueryService = actuatorCommandQueryService;
    this.actuatorCommandService = actuatorCommandService;
    this.iotNodeQueryService = iotNodeQueryService;
  }

  @Deprecated
  @GetMapping("/pending")
  @Operation(summary = "Get pending actuator commands for an Edge node",
      description = "No utilizado por el flujo actual, reemplazado por /sync/status y /actuators/desired",
      deprecated = true)
  public ResponseEntity<List<ActuatorCommandResource>> getPendingCommands(
      @PathVariable String nodeCode) {
    var node = iotNodeQueryService.handle(new GetNodeByNodeCodeQuery(nodeCode));
    if (node.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var commands = actuatorCommandQueryService
        .handle(new GetPendingActuatorCommandsByNodeIdQuery(node.get().getId()))
        .stream()
        .map(ActuatorCommandResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(commands);
  }

  @Deprecated
  @PutMapping("/{commandId}/complete")
  @Operation(summary = "Mark an actuator command as executed by the Edge node",
      description = "No utilizado por el flujo actual, reemplazado por /sync/status y /actuators/desired",
      deprecated = true)
  public ResponseEntity<MessageResource> completeCommand(@PathVariable String nodeCode,
                                                         @PathVariable Long commandId) {
    var node = iotNodeQueryService.handle(new GetNodeByNodeCodeQuery(nodeCode));
    if (node.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    actuatorCommandService.handle(new CompleteActuatorCommand(commandId));
    return ResponseEntity.ok(new MessageResource("Actuator command executed"));
  }
}
