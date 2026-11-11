package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.domain.model.queries.GetNodeByNodeCodeQuery;
import com.plantsync.platform.iot.domain.services.IoTNodeQueryService;
import com.plantsync.platform.iot.interfaces.rest.resources.ThresholdsResource;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetPlantByIdQuery;
import com.plantsync.platform.plantprofiles.domain.services.PlantQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/iot/edge/nodes/{nodeCode}/thresholds", produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Edge Thresholds", description = "Edge Service thresholds endpoint")
public class EdgeThresholdsController {

  private final IoTNodeQueryService iotNodeQueryService;
  private final PlantQueryService plantQueryService;

  public EdgeThresholdsController(IoTNodeQueryService iotNodeQueryService,
                                   PlantQueryService plantQueryService) {
    this.iotNodeQueryService = iotNodeQueryService;
    this.plantQueryService = plantQueryService;
  }

  @Deprecated
  @GetMapping
  @Operation(summary = "Get care thresholds for the plant linked to an edge node",
      description = "No utilizado por el flujo actual, reemplazado por /sync/config",
      deprecated = true)
  public ResponseEntity<ThresholdsResource> getThresholds(@PathVariable String nodeCode) {
    var nodeOpt = iotNodeQueryService.handle(new GetNodeByNodeCodeQuery(nodeCode));
    if (nodeOpt.isEmpty() || nodeOpt.get().getPlantId() == null) {
      return ResponseEntity.ok(new ThresholdsResource(false, null, null, null, null));
    }

    var plantOpt = plantQueryService.handle(new GetPlantByIdQuery(nodeOpt.get().getPlantId().value()));
    if (plantOpt.isEmpty()) {
      return ResponseEntity.ok(new ThresholdsResource(false, null, null, null, null));
    }

    var node = nodeOpt.get();
    var plant = plantOpt.get();
    return ResponseEntity.ok(new ThresholdsResource(
        true,
        node.getId(),
        plant.getTemperatureThresholdMin(),
        plant.getTemperatureThresholdMax(),
        plant.getLightThresholdMin()
    ));
  }
}
