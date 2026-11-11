package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.application.internal.commandservices.ActuatorCommandManualService;
import com.plantsync.platform.iot.interfaces.rest.assemblers.ActuatorCommandResourceFromEntityAssembler;
import com.plantsync.platform.iot.interfaces.rest.resources.ActuatorCommandResource;
import com.plantsync.platform.iot.interfaces.rest.resources.CreateActuatorCommandResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/iot/actuator-commands", produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Actuator Commands", description = "Manual actuator command endpoints")
public class ActuatorCommandManualController {

  private final ActuatorCommandManualService actuatorCommandManualService;

  public ActuatorCommandManualController(
      ActuatorCommandManualService actuatorCommandManualService) {
    this.actuatorCommandManualService = actuatorCommandManualService;
  }

  @PostMapping
  @Operation(
      summary = "Create a manual actuator command",
      description = "Send an actuator command to a node manually")
  @PreAuthorize("@access.node(#resource.nodeCode())")
  public ResponseEntity<ActuatorCommandResource> createActuatorCommand(
      @Valid @RequestBody CreateActuatorCommandResource resource) {
    if (resource.action()
            == com.plantsync.platform.iot.domain.model.valueobjects.ActuatorAction.ACTIVATE
        && resource.actuatorType()
            != com.plantsync.platform.iot.domain.model.valueobjects.ActuatorType.BUZZER) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT, "USE_TIMED_COMMAND");
    }
    try {
      var command =
          actuatorCommandManualService.handle(
              resource.nodeCode(), resource.actuatorType(), resource.action());
      var commandResource =
          ActuatorCommandResourceFromEntityAssembler.toResourceFromEntity(command);
      return new ResponseEntity<>(commandResource, HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
