package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.application.internal.commandservices.IoTNodeCommandServiceImpl;
import com.plantsync.platform.iot.interfaces.rest.assemblers.IoTNodeResourceFromEntityAssembler;
import com.plantsync.platform.iot.interfaces.rest.resources.IoTNodeResource;
import com.plantsync.platform.iot.interfaces.rest.resources.LinkPlantResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/iot/nodes", produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Nodes", description = "IoT Node Management Endpoints")
public class IoTNodeLinkController {

  private final IoTNodeCommandServiceImpl iotNodeCommandService;

  public IoTNodeLinkController(IoTNodeCommandServiceImpl iotNodeCommandService) {
    this.iotNodeCommandService = iotNodeCommandService;
  }

  @PatchMapping("/{nodeCode}/link-plant")
  @Operation(
      summary = "Link an IoT node to a plant",
      description = "Link an IoT node to a plant by plant ID")
  @PreAuthorize("@access.link(#nodeCode, #resource.plantId())")
  public ResponseEntity<IoTNodeResource> linkPlant(
      @PathVariable String nodeCode, @Valid @RequestBody LinkPlantResource resource) {
    try {
      var nodeOpt = iotNodeCommandService.linkPlant(nodeCode, resource.plantId());
      if (nodeOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      var nodeResource = IoTNodeResourceFromEntityAssembler.toResourceFromEntity(nodeOpt.get());
      return ResponseEntity.ok(nodeResource);
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @PostMapping("/{nodeCode}/unlink")
  @Operation(
      summary = "Unlink an IoT node from its plant",
      description = "Remove the plant link from an IoT node")
  @PreAuthorize("@access.node(#nodeCode)")
  public ResponseEntity<IoTNodeResource> unlinkPlant(@PathVariable String nodeCode) {
    var nodeOpt = iotNodeCommandService.unlinkPlant(nodeCode);
    if (nodeOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var nodeResource = IoTNodeResourceFromEntityAssembler.toResourceFromEntity(nodeOpt.get());
    return ResponseEntity.ok(nodeResource);
  }
}
