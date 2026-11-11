package com.plantsync.platform.plantprofiles.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.plantprofiles.domain.model.queries.GetAllPlantsByProfileIdQuery;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetAllPlantsQuery;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetPlantByIdQuery;
import com.plantsync.platform.plantprofiles.domain.services.PlantQueryService;
import com.plantsync.platform.plantprofiles.interfaces.rest.assemblers.PlantResourceFromEntityAssembler;
import com.plantsync.platform.plantprofiles.interfaces.rest.resources.PlantResource;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for querying plant entities. */
@RestController
@RequestMapping(value = "/api/v1/plants", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Plants", description = "Plant Management Endpoints")
public class PlantQueryController {

  private final PlantQueryService plantQueryService;

  /**
   * Constructor for PlantQueryController.
   *
   * @param plantQueryService The plant query service.
   */
  public PlantQueryController(PlantQueryService plantQueryService) {
    this.plantQueryService = plantQueryService;
  }

  /**
   * Gets all plants.
   *
   * @return A list of all plants.
   */
  @GetMapping
  @Operation(summary = "Get all plants")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Plants found"),
    @ApiResponse(responseCode = "404", description = "No plants found")
  })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<PlantResource>> getAllPlants() {
    var plants = plantQueryService.handle(new GetAllPlantsQuery());

    if (plants.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var resources =
        plants.stream().map(PlantResourceFromEntityAssembler::toResourceFromEntity).toList();

    return ResponseEntity.ok(resources);
  }

  /**
   * Gets all plants by profile ID.
   *
   * @param profileId The ID of the profile.
   * @return A list of plants owned by the profile.
   */
  @GetMapping("/by-profile/{profileId}")
  @Operation(summary = "Get plants by profile ID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Plants found for the user"),
    @ApiResponse(responseCode = "404", description = "No plants found for the user")
  })
  @PreAuthorize("@access.profile(#profileId)")
  public ResponseEntity<List<PlantResource>> getAllPlantsByProfileId(@PathVariable Long profileId) {
    var plants =
        plantQueryService.handle(new GetAllPlantsByProfileIdQuery(new ProfileId(profileId)));

    if (plants.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var resources =
        plants.stream().map(PlantResourceFromEntityAssembler::toResourceFromEntity).toList();

    return ResponseEntity.ok(resources);
  }

  /**
   * Gets a plant by its ID.
   *
   * @param plantId The ID of the plant.
   * @return The plant if found.
   */
  @GetMapping("/{plantId}")
  @Operation(summary = "Get plant by ID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Plant found for the user"),
    @ApiResponse(responseCode = "404", description = "No plant found for the user")
  })
  @PreAuthorize("@access.plant(#plantId)")
  public ResponseEntity<PlantResource> getPlantById(@PathVariable Long plantId) {
    var getPlantByIdQuery = new GetPlantByIdQuery(plantId);
    var plant = plantQueryService.handle(getPlantByIdQuery);
    if (plant.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var plantEntity = plant.get();
    var plantResource = PlantResourceFromEntityAssembler.toResourceFromEntity(plantEntity);
    return ResponseEntity.ok(plantResource);
  }
}
