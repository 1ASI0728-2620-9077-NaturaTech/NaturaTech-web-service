package com.plantsync.platform.profiles.interfaces.rest;

import com.plantsync.platform.profiles.domain.services.ProfileCommandService;
import com.plantsync.platform.profiles.interfaces.rest.resources.CreateProfileResource;
import com.plantsync.platform.profiles.interfaces.rest.resources.ProfileResource;
import com.plantsync.platform.profiles.interfaces.rest.resources.UpdateProfileResource;
import com.plantsync.platform.profiles.interfaces.rest.transform.CreateProfileCommandFromResourceAssembler;
import com.plantsync.platform.profiles.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import com.plantsync.platform.profiles.interfaces.rest.transform.UpdateProfileCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The type Profiles command controller. */
@RestController
@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profiles", description = "Available Profile Endpoints")
public class ProfilesCommandController {
  private final ProfileCommandService profileCommandService;

  /**
   * Constructor for ProfilesCommandController.
   *
   * @param profileCommandService The profile command service.
   */
  public ProfilesCommandController(ProfileCommandService profileCommandService) {
    this.profileCommandService = profileCommandService;
  }

  /**
   * Create a new profile.
   *
   * @param resource The {@link CreateProfileResource} instance.
   * @return A {@link ProfileResource} for the created profile, or a bad request response.
   */
  @PostMapping
  @Operation(summary = "Create a new profile")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Profile created"),
        @ApiResponse(responseCode = "400", description = "Bad request")
      })
  @PreAuthorize(
      "@access.user(#resource.userId()) and (hasRole('ADMIN') or #resource.subscriptionPlan() =="
          + " 'BASIC')")
  public ResponseEntity<ProfileResource> createProfile(
      @RequestBody CreateProfileResource resource) {
    var createProfileCommand =
        CreateProfileCommandFromResourceAssembler.toCommandFromResource(resource);
    var profile = profileCommandService.handle(createProfileCommand);
    if (profile.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    var createdProfile = profile.get();
    var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(createdProfile);
    return new ResponseEntity<>(profileResource, HttpStatus.CREATED);
  }

  /**
   * Updates an existing profile by its ID.
   *
   * @param id The ID of the profile.
   * @param resource The {@link UpdateProfileResource} instance.
   * @return The updated {@link ProfileResource}.
   */
  @PutMapping("/{id}")
  @Operation(summary = "Update a profile by ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "404", description = "Profile not found")
      })
  @PreAuthorize("@access.plan(#id, #resource.subscriptionPlan())")
  public ResponseEntity<ProfileResource> updateProfile(
      @PathVariable Long id, @RequestBody UpdateProfileResource resource) {
    var command = UpdateProfileCommandFromResourceAssembler.toCommandFromResource(id, resource);
    var profile = profileCommandService.handle(command);
    if (profile.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile.get());
    return ResponseEntity.ok(profileResource);
  }
}
