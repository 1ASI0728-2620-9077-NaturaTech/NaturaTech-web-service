package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.domain.services.SensorReadingCommandService;
import com.plantsync.platform.iot.interfaces.rest.assemblers.CreateReadingCommandFromResourceAssembler;
import com.plantsync.platform.iot.interfaces.rest.resources.BatchSensorReadingResponseResource;
import com.plantsync.platform.iot.interfaces.rest.resources.CreateReadingResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/iot/edge/sensor-readings", produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Edge Sensor Readings", description = "Edge Service batch ingestion endpoints")
public class EdgeSensorReadingController {

  private final SensorReadingCommandService sensorReadingCommandService;

  public EdgeSensorReadingController(SensorReadingCommandService sensorReadingCommandService,
      com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository nodes,
      com.plantsync.platform.iot.security.NodeCredentials credentials,
      jakarta.validation.Validator validator) {
    this.validator = validator;
    this.nodes = nodes;
    this.credentials = credentials;
    this.sensorReadingCommandService = sensorReadingCommandService;
  }

  private final com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository nodes;
  private final com.plantsync.platform.iot.security.NodeCredentials credentials;
  private final jakarta.validation.Validator validator;

  @PostMapping(value = "/batch", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Ingest sensor readings in batch from the Edge Service")
  @org.springframework.transaction.annotation.Transactional
  public ResponseEntity<BatchSensorReadingResponseResource> createReadingsBatch(
      @org.springframework.web.bind.annotation.RequestHeader("X-Node-Key") String key,
      @RequestBody List<@Valid CreateReadingResource> resources) {
    if (resources == null || resources.isEmpty() || resources.size() > 100)
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "BATCH_SIZE_INVALID");
    for (var resource : resources) {
      if (resource == null || !validator.validate(resource).isEmpty())
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "READING_INVALID");
    }
    var nodeIds = resources.stream().map(CreateReadingResource::nodeId).distinct().toList();
    if (nodeIds.size() != 1 || nodeIds.get(0) == null)
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "SINGLE_NODE_REQUIRED");
    var node = nodes.findById(nodeIds.get(0)).orElseThrow(() ->
        new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND"));
    credentials.verify(node.getNodeCode(), key);
    var readingIds = resources.stream()
        .map(CreateReadingCommandFromResourceAssembler::toCommandFromResource)
        .map(sensorReadingCommandService::handle)
        .map(reading -> reading.getId())
        .toList();
    var response = new BatchSensorReadingResponseResource("Sensor readings ingested", readingIds);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
}
