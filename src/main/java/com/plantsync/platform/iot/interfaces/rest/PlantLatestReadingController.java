package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotSensorReadingRepository;
import com.plantsync.platform.iot.interfaces.rest.assemblers.SensorReadingResourceFromEntityAssembler;
import com.plantsync.platform.iot.interfaces.rest.resources.SensorReadingResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Query endpoints exposing the latest raw sensor reading for a plant.
 *
 * <p>The dashboard global telemetry works with averages; the web and mobile apps need the last raw
 * values (light, gas, air temperature) to show the plant's current state in real time. Readings are
 * stored per node, so this controller resolves the node linked to the plant and returns its most
 * recent reading.
 */
@RestController
@RequestMapping(value = "/api/v1/iot/plants/{plantId}/readings", produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Plant Readings", description = "Latest raw sensor reading queries for a plant")
public class PlantLatestReadingController {

  private final IotNodeRepository iotNodeRepository;
  private final IotSensorReadingRepository iotSensorReadingRepository;

  public PlantLatestReadingController(
      IotNodeRepository iotNodeRepository, IotSensorReadingRepository iotSensorReadingRepository) {
    this.iotNodeRepository = iotNodeRepository;
    this.iotSensorReadingRepository = iotSensorReadingRepository;
  }

  @GetMapping("/latest")
  @Operation(
      summary = "Get the latest raw sensor reading for a plant",
      description =
          "Returns the most recent raw values (light, gas, air temperature) reported by "
              + "the node linked to the plant, for real-time display in the web and mobile apps.")
  @PreAuthorize("@access.plant(#plantId)")
  public ResponseEntity<SensorReadingResource> getLatestReading(@PathVariable Long plantId) {
    var nodeOpt = iotNodeRepository.findByPlantIdValue(plantId);
    if (nodeOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var readingOpt =
        iotSensorReadingRepository.findTopByNodeIdOrderByTimestampDesc(nodeOpt.get().getId());
    return readingOpt
        .map(SensorReadingResourceFromEntityAssembler::toResourceFromEntity)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
