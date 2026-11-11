package com.plantsync.platform.iot.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.iot.domain.model.queries.GetAllNodesByProfileIdQuery;
import com.plantsync.platform.iot.domain.services.IoTNodeCommandService;
import com.plantsync.platform.iot.domain.services.IoTNodeQueryService;
import com.plantsync.platform.iot.interfaces.rest.assemblers.CreateNodeCommandFromResourceAssembler;
import com.plantsync.platform.iot.interfaces.rest.assemblers.IoTNodeResourceFromEntityAssembler;
import com.plantsync.platform.iot.interfaces.rest.resources.CreateNodeResource;
import com.plantsync.platform.iot.interfaces.rest.resources.IoTNodeResource;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/iot/nodes", produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Nodes", description = "IoT Node Management Endpoints")
public class IoTNodeController {

  private final IoTNodeCommandService iotNodeCommandService;
  private final IoTNodeQueryService iotNodeQueryService;

  public IoTNodeController(
      IoTNodeCommandService iotNodeCommandService, IoTNodeQueryService iotNodeQueryService) {
    this.iotNodeCommandService = iotNodeCommandService;
    this.iotNodeQueryService = iotNodeQueryService;
  }

  @PostMapping
  @Operation(summary = "Register a new IoT node", description = "Register a new IoT node")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "IoT node created"),
        @ApiResponse(responseCode = "400", description = "Invalid input or duplicate node code")
      })
  @PreAuthorize("@access.createNode(#resource.profileId(), #resource.plantId())")
  public ResponseEntity<IoTNodeResource> createNode(
      @Valid @RequestBody CreateNodeResource resource) {
    var command = CreateNodeCommandFromResourceAssembler.toCommandFromResource(resource);
    var node = iotNodeCommandService.handle(command);
    if (node.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    var nodeEntity = node.get();
    var nodeResource = IoTNodeResourceFromEntityAssembler.toResourceFromEntity(nodeEntity);
    return new ResponseEntity<>(nodeResource, HttpStatus.CREATED);
  }

  @GetMapping("/by-profile/{profileId}")
  @Operation(
      summary = "Get nodes by profile ID",
      description = "Get all IoT nodes for a given profile")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Nodes found for the profile"),
        @ApiResponse(responseCode = "404", description = "No nodes found for the profile")
      })
  @PreAuthorize("@access.profile(#profileId)")
  public ResponseEntity<List<IoTNodeResource>> getNodesByProfileId(@PathVariable Long profileId) {
    var nodes =
        iotNodeQueryService.handle(new GetAllNodesByProfileIdQuery(new ProfileId(profileId)));
    if (nodes.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var resources =
        nodes.stream().map(IoTNodeResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(resources);
  }
}
