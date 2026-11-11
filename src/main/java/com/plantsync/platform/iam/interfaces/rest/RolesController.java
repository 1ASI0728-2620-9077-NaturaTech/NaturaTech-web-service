package com.plantsync.platform.iam.interfaces.rest;

import com.plantsync.platform.iam.domain.model.queries.GetAllRolesQuery;
import com.plantsync.platform.iam.domain.services.RoleQueryService;
import com.plantsync.platform.iam.interfaces.rest.resources.RoleResource;
import com.plantsync.platform.iam.interfaces.rest.transform.RoleResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing roles. Provides endpoints to retrieve all available roles in the
 * system.
 */
@RestController
@RequestMapping(value = "/api/v1/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Roles", description = "Available Role Endpoints")
public class RolesController {
  private final RoleQueryService roleQueryService;

  /**
   * Constructor for RolesController.
   *
   * @param roleQueryService The role query service.
   */
  public RolesController(RoleQueryService roleQueryService) {
    this.roleQueryService = roleQueryService;
  }

  /**
   * Get all roles.
   *
   * @return List of role resources
   * @see RoleResource
   */
  @GetMapping
  @Operation(summary = "Get all roles", description = "Get all the roles available in the system.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Roles retrieved successfully."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
      })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<RoleResource>> getAllRoles() {
    var getAllRolesQuery = new GetAllRolesQuery();
    var roles = roleQueryService.handle(getAllRolesQuery);
    var roleResources =
        roles.stream().map(RoleResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(roleResources);
  }
}
