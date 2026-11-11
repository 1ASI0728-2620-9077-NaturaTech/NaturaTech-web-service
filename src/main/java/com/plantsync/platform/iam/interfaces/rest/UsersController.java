package com.plantsync.platform.iam.interfaces.rest;

import com.plantsync.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.plantsync.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.plantsync.platform.iam.domain.services.UserCommandService;
import com.plantsync.platform.iam.domain.services.UserQueryService;
import com.plantsync.platform.iam.interfaces.rest.resources.UpdateUserResource;
import com.plantsync.platform.iam.interfaces.rest.resources.UserResource;
import com.plantsync.platform.iam.interfaces.rest.transform.UpdateUserCommandFromResourceAssembler;
import com.plantsync.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing users.
 *
 * <p>Provides endpoints to retrieve and update user information.
 */
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Available User Endpoints")
public class UsersController {
  private final UserQueryService userQueryService;
  private final UserCommandService userCommandService;

  /**
   * Constructor for UsersController.
   *
   * @param userQueryService The user query service.
   * @param userCommandService The user command service.
   */
  public UsersController(UserQueryService userQueryService, UserCommandService userCommandService) {
    this.userQueryService = userQueryService;
    this.userCommandService = userCommandService;
  }

  /**
   * Retrieves all users in the system.
   *
   * @return A list of {@link UserResource} objects.
   */
  @GetMapping
  @Operation(summary = "Get all users", description = "Get all the users available in the system.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
      })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResource>> getAllUsers() {
    var getAllUsersQuery = new GetAllUsersQuery();
    var users = userQueryService.handle(getAllUsersQuery);
    var userResources =
        users.stream().map(UserResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(userResources);
  }

  /**
   * Retrieves a user by their ID.
   *
   * @param userId The ID of the user to retrieve.
   * @return The {@link UserResource} with the given ID.
   */
  @GetMapping(value = "/{userId}")
  @Operation(summary = "Get user by id", description = "Get the user with the given id.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User retrieved successfully."),
        @ApiResponse(responseCode = "404", description = "User not found."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
      })
  @PreAuthorize("@access.user(#userId)")
  public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
    var getUserByIdQuery = new GetUserByIdQuery(userId);
    var user = userQueryService.handle(getUserByIdQuery);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
    return ResponseEntity.ok(userResource);
  }

  /**
   * Updates a user's information by ID.
   *
   * @param id The ID of the user to update.
   * @param resource The {@link UpdateUserResource} data.
   * @return The updated {@link UserResource}.
   */
  @PutMapping("/{id}")
  @Operation(summary = "Update a user by ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @PreAuthorize("@access.user(#id)")
  public ResponseEntity<UserResource> updateUser(
      @Valid @PathVariable Long id, @Valid @RequestBody UpdateUserResource resource) {

    var command = UpdateUserCommandFromResourceAssembler.toCommandFromResource(id, resource);
    var updatedUser = userCommandService.handle(command);

    if (updatedUser.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(updatedUser.get());
    return ResponseEntity.ok(userResource);
  }
}
