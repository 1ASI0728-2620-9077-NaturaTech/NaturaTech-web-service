package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.domain.model.queries.GetNodeByIdQuery;
import com.plantsync.platform.iot.domain.services.IoTNodeQueryService;
import com.plantsync.platform.iot.domain.services.SensorReadingCommandService;
import com.plantsync.platform.iot.interfaces.rest.assemblers.CreateReadingCommandFromResourceAssembler;
import com.plantsync.platform.iot.interfaces.rest.assemblers.SensorReadingResourceFromEntityAssembler;
import com.plantsync.platform.iot.interfaces.rest.resources.CreateReadingResource;
import com.plantsync.platform.iot.interfaces.rest.resources.SensorReadingResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping(value = "/api/v1/iot/readings", produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Sensor Readings", description = "Sensor Reading Ingestion Endpoints")
public class SensorReadingController {

  private final SensorReadingCommandService sensorReadingCommandService;
  private final IoTNodeQueryService iotNodeQueryService;

  public SensorReadingController(
      SensorReadingCommandService sensorReadingCommandService,
      IoTNodeQueryService iotNodeQueryService) {
    this.sensorReadingCommandService = sensorReadingCommandService;
    this.iotNodeQueryService = iotNodeQueryService;
  }

  @PostMapping
  @Operation(
      summary = "Ingest a sensor reading",
      description = "Process a sensor reading and evaluate actuator rules")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Reading processed"),
        @ApiResponse(responseCode = "400", description = "Invalid input or unknown node")
      })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SensorReadingResource> createReading(
      @Valid @RequestBody CreateReadingResource resource) {
    var node = iotNodeQueryService.handle(new GetNodeByIdQuery(resource.nodeId()));
    if (node.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    var command = CreateReadingCommandFromResourceAssembler.toCommandFromResource(resource);
    var reading = sensorReadingCommandService.handle(command);
    var readingResource = SensorReadingResourceFromEntityAssembler.toResourceFromEntity(reading);
    return new ResponseEntity<>(readingResource, HttpStatus.CREATED);
  }
}
