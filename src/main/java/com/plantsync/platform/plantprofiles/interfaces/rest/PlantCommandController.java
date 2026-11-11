package com.plantsync.platform.plantprofiles.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.plantprofiles.domain.model.commands.DeletePlantCommand;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetPlantByIdQuery;
import com.plantsync.platform.plantprofiles.domain.services.PlantCommandService;
import com.plantsync.platform.plantprofiles.domain.services.PlantQueryService;
import com.plantsync.platform.plantprofiles.interfaces.rest.assemblers.CreatePlantCommandFromResourceAssembler;
import com.plantsync.platform.plantprofiles.interfaces.rest.assemblers.PlantResourceFromEntityAssembler;
import com.plantsync.platform.plantprofiles.interfaces.rest.assemblers.UpdatePlantCommandFromResourceAssembler;
import com.plantsync.platform.plantprofiles.interfaces.rest.resources.CreatePlantResource;
import com.plantsync.platform.plantprofiles.interfaces.rest.resources.PlantResource;
import com.plantsync.platform.plantprofiles.interfaces.rest.resources.UpdatePlantResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling HTTP requests related to Plant creation and management. Provides
 * endpoints to create, delete, and update plants.
 */
@RestController
@RequestMapping(value = "/api/v1/plants", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Plants", description = "Available Plant Endpoints")
public class PlantCommandController {

  private final PlantCommandService plantCommandService;
  private final PlantQueryService plantQueryService;

  /**
   * Constructor for PlantCommandController.
   *
   * @param plantCommandService The plant command service.
   * @param plantQueryService The plant query service.
   */
  public PlantCommandController(
      PlantCommandService plantCommandService, PlantQueryService plantQueryService) {
    this.plantCommandService = plantCommandService;
    this.plantQueryService = plantQueryService;
  }

  /**
   * Create a new plant.
   *
   * @param resource The plant resource.
   * @return The created plant resource.
   */
  @PostMapping
  @Operation(summary = "Create a new plant", description = "Create a new plant")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Plant created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Plant not found")
      })
  @PreAuthorize("@access.profile(#resource.profileId())")
  public ResponseEntity<PlantResource> createPlant(
      @Valid @RequestBody CreatePlantResource resource) {
    var createPlantCommand =
        CreatePlantCommandFromResourceAssembler.toCommandFromResource(resource);
    var plantId = plantCommandService.handle(createPlantCommand);
    if (plantId == null || plantId == 0L) {
      return ResponseEntity.badRequest().build();
    }
    var getPlantByIdQuery = new GetPlantByIdQuery(plantId);
    var plant = plantQueryService.handle(getPlantByIdQuery);
    if (plant.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var plantEntity = plant.get();
    var plantResource = PlantResourceFromEntityAssembler.toResourceFromEntity(plantEntity);
    return new ResponseEntity<>(plantResource, HttpStatus.CREATED);
  }

  /**
   * Delete plant.
   *
   * @param plantId The plant id.
   * @return A {@link ResponseEntity} with the result of the deletion.
   */
  @DeleteMapping("/{plantId}")
  @Operation(summary = "Delete plant", description = "Delete plant with a given ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Plant deleted"),
        @ApiResponse(responseCode = "404", description = "Plant not found")
      })
  @PreAuthorize("@access.plant(#plantId)")
  public ResponseEntity<?> deletePlant(@PathVariable Long plantId) {
    var deletePlantCommand = new DeletePlantCommand(plantId);
    plantCommandService.handle(deletePlantCommand);
    return ResponseEntity.ok("Plant with id successfully deleted");
  }

  /**
   * Update a plant.
   *
   * @param plantId The plant id.
   * @param resource The update plant resource.
   * @return The updated plant resource.
   */
  @PutMapping("/{plantId}")
  @Operation(summary = "Update a plant")
  @PreAuthorize(
      "@access.plant(#plantId) and (#resource.profileId() == null or @access.plantProfile(#plantId,"
          + " #resource.profileId()))")
  public ResponseEntity<PlantResource> updatePlant(
      @PathVariable Long plantId, @RequestBody UpdatePlantResource resource) {
    var command = UpdatePlantCommandFromResourceAssembler.toCommandFromResource(plantId, resource);
    var updated = plantCommandService.handle(command);

    if (updated.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var resourceResponse = PlantResourceFromEntityAssembler.toResourceFromEntity(updated.get());
    return ResponseEntity.ok(resourceResponse);
  }
}
