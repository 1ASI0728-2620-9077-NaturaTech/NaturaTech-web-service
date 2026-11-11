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

  public EdgeSensorReadingController(SensorReadingCommandService sensorReadingCommandService) {
    this.sensorReadingCommandService = sensorReadingCommandService;
  }

  @PostMapping(value = "/batch", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Ingest sensor readings in batch from the Edge Service")
  public ResponseEntity<BatchSensorReadingResponseResource> createReadingsBatch(
      @Valid @RequestBody List<CreateReadingResource> resources) {
    var readingIds = resources.stream()
        .map(CreateReadingCommandFromResourceAssembler::toCommandFromResource)
        .map(sensorReadingCommandService::handle)
        .map(reading -> reading.getId())
        .toList();
    var response = new BatchSensorReadingResponseResource("Sensor readings ingested", readingIds);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
}
