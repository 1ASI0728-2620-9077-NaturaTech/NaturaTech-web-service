package com.plantsync.platform.profiles.interfaces.rest;

import com.plantsync.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.plantsync.platform.profiles.domain.services.ProfileQueryService;
import com.plantsync.platform.profiles.interfaces.rest.resources.ProfileResource;
import com.plantsync.platform.profiles.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for querying profiles. */
@RestController
@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profiles", description = "Available Profile Endpoints")
public class ProfilesQueryController {

  private final ProfileQueryService profileQueryService;

  /**
   * Constructor for ProfilesQueryController.
   *
   * @param profileQueryService The profile query service.
   */
  public ProfilesQueryController(ProfileQueryService profileQueryService) {
    this.profileQueryService = profileQueryService;
  }

  /**
   * Gets a profile by its ID.
   *
   * @param profileId The ID of the profile.
   * @return The {@link ProfileResource} if found.
   */
  @GetMapping("/{profileId}")
  @Operation(summary = "Get a profile by ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Profile found"),
        @ApiResponse(responseCode = "404", description = "Profile not found")
      })
  @PreAuthorize("@access.profile(#profileId)")
  public ResponseEntity<ProfileResource> getProfileById(@PathVariable Long profileId) {
    var getProfileByIdQuery = new GetProfileByIdQuery(profileId);
    var profile = profileQueryService.handle(getProfileByIdQuery);
    if (profile.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var profileEntity = profile.get();
    var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(profileEntity);
    return ResponseEntity.ok(profileResource);
  }

  /**
   * Gets all profiles.
   *
   * @return A list of {@link ProfileResource} resources.
   */
  @GetMapping
  @Operation(summary = "Get all profiles")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Profiles found"),
        @ApiResponse(responseCode = "404", description = "Profiles not found")
      })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<ProfileResource>> getAllProfiles() {
    var profiles = profileQueryService.handle(new GetAllProfilesQuery());
    if (profiles.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var profileResources =
        profiles.stream().map(ProfileResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(profileResources);
  }

  /**
   * Retrieves a profile by its associated user ID.
   *
   * @param userId The ID of the user.
   * @return The {@link ProfileResource} if found.
   */
  @GetMapping("/by-user-id")
  @Operation(summary = "Get profile by user ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Profile found"),
        @ApiResponse(responseCode = "404", description = "Profile not found")
      })
  @PreAuthorize("@access.user(#userId)")
  public ResponseEntity<ProfileResource> getProfileByUserId(@RequestParam Long userId) {
    var query = new GetProfileByUserIdQuery(userId);
    var profile = profileQueryService.handle(query);

    if (profile.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile.get());
    return ResponseEntity.ok(profileResource);
  }
}
