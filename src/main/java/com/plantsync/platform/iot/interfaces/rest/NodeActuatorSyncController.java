package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.domain.model.queries.GetNodeByNodeCodeQuery;
import com.plantsync.platform.iot.domain.model.valueobjects.BuzzerMode;
import com.plantsync.platform.iot.domain.model.valueobjects.DesiredActuatorState;
import com.plantsync.platform.iot.domain.model.valueobjects.LedMode;
import com.plantsync.platform.iot.domain.model.valueobjects.ReportedActuatorState;
import com.plantsync.platform.iot.domain.model.valueobjects.ServoMode;
import com.plantsync.platform.iot.domain.services.IoTNodeQueryService;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository;
import com.plantsync.platform.iot.interfaces.rest.resources.ActuatorStateResource;
import com.plantsync.platform.iot.interfaces.rest.resources.NodeSyncConfigResource;
import com.plantsync.platform.iot.interfaces.rest.resources.NodeSyncStatusResource;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetPlantByIdQuery;
import com.plantsync.platform.plantprofiles.domain.services.PlantQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/iot/nodes", produces = APPLICATION_JSON_VALUE)
@Tag(
    name = "IoT Node Actuator Sync",
    description = "Node actuator state management and device sync endpoints")
public class NodeActuatorSyncController {

  private final IoTNodeQueryService iotNodeQueryService;
  private final IotNodeRepository iotNodeRepository;
  private final PlantQueryService plantQueryService;

  public NodeActuatorSyncController(
      IoTNodeQueryService iotNodeQueryService,
      IotNodeRepository iotNodeRepository,
      PlantQueryService plantQueryService) {
    this.iotNodeQueryService = iotNodeQueryService;
    this.iotNodeRepository = iotNodeRepository;
    this.plantQueryService = plantQueryService;
  }

  @PutMapping("/{nodeCode}/actuators/desired")
  @Operation(
      summary = "Update desired actuator state",
      description = "Set the desired actuator state for a node")
  @PreAuthorize("@access.node(#nodeCode)")
  public ResponseEntity<ActuatorStateResource> updateDesiredState(
      @PathVariable String nodeCode, @RequestBody ActuatorStateResource resource) {
    if (!"OFF".equalsIgnoreCase(resource.servoMode())
        || !"OFF".equalsIgnoreCase(resource.ledMode())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT, "USE_TIMED_COMMAND");
    }
    var nodeOpt = iotNodeQueryService.handle(new GetNodeByNodeCodeQuery(nodeCode));
    if (nodeOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var node = nodeOpt.get();
    var desiredState =
        new DesiredActuatorState(
            BuzzerMode.valueOf(resource.buzzerMode().toUpperCase()),
            ServoMode.valueOf(resource.servoMode().toUpperCase()),
            LedMode.valueOf(resource.ledMode().toUpperCase()));
    node.setDesiredState(desiredState);
    iotNodeRepository.save(node);
    return ResponseEntity.ok(
        new ActuatorStateResource(
            desiredState.buzzerMode().name(),
            desiredState.servoMode().name(),
            desiredState.ledMode().name()));
  }

  @PutMapping("/{nodeCode}/actuators/reported")
  @Operation(
      summary = "Report actual actuator state",
      description = "Report the actual actuator state from the device")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ActuatorStateResource> updateReportedState(
      @PathVariable String nodeCode, @RequestBody ActuatorStateResource resource) {
    var nodeOpt = iotNodeQueryService.handle(new GetNodeByNodeCodeQuery(nodeCode));
    if (nodeOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var node = nodeOpt.get();
    var reportedState =
        new ReportedActuatorState(
            BuzzerMode.valueOf(resource.buzzerMode().toUpperCase()),
            ServoMode.valueOf(resource.servoMode().toUpperCase()),
            LedMode.valueOf(resource.ledMode().toUpperCase()));
    node.setReportedState(reportedState);
    iotNodeRepository.save(node);
    return ResponseEntity.ok(
        new ActuatorStateResource(
            reportedState.buzzerMode().name(),
            reportedState.servoMode().name(),
            reportedState.ledMode().name()));
  }

  @GetMapping("/{nodeCode}/sync/status")
  @Operation(
      summary = "Get node sync status",
      description =
          "Get the current sync status and desired state for a node (polled at high frequency by"
              + " device)")
  @PreAuthorize("@access.node(#nodeCode)")
  public ResponseEntity<NodeSyncStatusResource> getSyncStatus(@PathVariable String nodeCode) {
    var nodeOpt = iotNodeQueryService.handle(new GetNodeByNodeCodeQuery(nodeCode));
    if (nodeOpt.isEmpty()) {
      return ResponseEntity.ok(
          new NodeSyncStatusResource(false, new ActuatorStateResource("OFF", "OFF", "OFF")));
    }
    var node = nodeOpt.get();
    var desired = node.getDesiredState();
    return ResponseEntity.ok(
        new NodeSyncStatusResource(
            node.getPlantId() != null,
            new ActuatorStateResource(
                desired.buzzerMode().name(),
                desired.servoMode().name(),
                desired.ledMode().name())));
  }

  @GetMapping("/{nodeCode}/sync/config")
  @Operation(
      summary = "Get node sync config",
      description = "Get the full configuration for a node (polled at boot or on link change)")
  @PreAuthorize("@access.node(#nodeCode)")
  public ResponseEntity<NodeSyncConfigResource> getSyncConfig(@PathVariable String nodeCode) {
    var nodeOpt = iotNodeQueryService.handle(new GetNodeByNodeCodeQuery(nodeCode));
    if (nodeOpt.isEmpty() || nodeOpt.get().getPlantId() == null) {
      return ResponseEntity.ok(
          new NodeSyncConfigResource(false, null, null, null, null, null, null, null));
    }
    var node = nodeOpt.get();
    var plantOpt = plantQueryService.handle(new GetPlantByIdQuery(node.getPlantId().value()));
    if (plantOpt.isEmpty()) {
      return ResponseEntity.ok(
          new NodeSyncConfigResource(false, null, null, null, null, null, null, null));
    }
    var plant = plantOpt.get();
    return ResponseEntity.ok(
        new NodeSyncConfigResource(
            true,
            node.getId(),
            plant.getSpecies(),
            plant.getTemperatureThresholdMin(),
            plant.getTemperatureThresholdMax(),
            plant.getHumidityThresholdMin(),
            plant.getHumidityThresholdMax(),
            plant.getLightThresholdMin()));
  }
}
