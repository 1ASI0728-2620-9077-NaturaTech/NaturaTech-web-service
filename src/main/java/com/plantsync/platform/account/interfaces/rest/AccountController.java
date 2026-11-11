package com.plantsync.platform.account.interfaces.rest;

import com.plantsync.platform.account.interfaces.rest.resources.UpdateAccountResource;
import com.plantsync.platform.iam.domain.model.commands.UpdateUserCommand;
import com.plantsync.platform.iam.domain.model.queries.GetUserByEmailQuery;
import com.plantsync.platform.iam.domain.services.UserCommandService;
import com.plantsync.platform.iam.domain.services.UserQueryService;
import com.plantsync.platform.profiles.domain.model.commands.UpdateProfileCommand;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.plantsync.platform.profiles.domain.services.ProfileCommandService;
import com.plantsync.platform.profiles.domain.services.ProfileQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Facade controller for account-level operations spanning IAM and profiles.
 */
@RestController
@RequestMapping(value = "/api/v1/account", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Account", description = "Authenticated account endpoints")
public class AccountController {

  private final UserCommandService userCommandService;
  private final UserQueryService userQueryService;
  private final ProfileCommandService profileCommandService;
  private final ProfileQueryService profileQueryService;

  /**
   * Creates the account facade controller.
   *
   * @param userCommandService    The user command service.
   * @param userQueryService      The user query service.
   * @param profileCommandService The profile command service.
   * @param profileQueryService   The profile query service.
   */
  public AccountController(UserCommandService userCommandService,
                           UserQueryService userQueryService,
                           ProfileCommandService profileCommandService,
                           ProfileQueryService profileQueryService) {
    this.userCommandService = userCommandService;
    this.userQueryService = userQueryService;
    this.profileCommandService = profileCommandService;
    this.profileQueryService = profileQueryService;
  }

  /**
   * Updates the authenticated user's account data in IAM and profiles.
   *
   * @param resource       The account update resource.
   * @param authentication The current Spring Security authentication.
   * @return An empty 200 response if the account was updated.
   */
  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @Operation(summary = "Update authenticated account")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Account updated"),
      @ApiResponse(responseCode = "400", description = "Invalid account data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "User or profile not found")
  })
  public ResponseEntity<Void> updateAccount(
      @RequestBody UpdateAccountResource resource,
      Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var currentEmail = authentication.getName();
    var currentUser = userQueryService.handle(new GetUserByEmailQuery(currentEmail));
    if (currentUser.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var user = currentUser.get();
    var profile = profileQueryService.handle(new GetProfileByUserIdQuery(user.getId()));
    if (profile.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var personName = toPersonName(resource.firstName(), resource.lastName());
    if (personName.isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    var userUpdate = userCommandService.handle(
        new UpdateUserCommand(user.getId(), resource.email()));
    if (userUpdate.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var profileToUpdate = profile.get();
    var profileUpdate = profileCommandService.handle(
        new UpdateProfileCommand(
            profileToUpdate.getId(),
            personName,
            profileToUpdate.getSubscriptionPlan().name(),
            resource.profilePictureBase64()));
    if (profileUpdate.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok().build();
  }

  private String toPersonName(String firstName, String lastName) {
    return Stream.of(firstName, lastName)
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .collect(Collectors.joining(" "));
  }
}
