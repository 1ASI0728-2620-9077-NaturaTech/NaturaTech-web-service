package com.plantsync.platform.account.interfaces.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plantsync.platform.account.interfaces.rest.resources.UpdateAccountResource;
import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.domain.model.commands.UpdateUserCommand;
import com.plantsync.platform.iam.domain.model.queries.GetUserByEmailQuery;
import com.plantsync.platform.iam.domain.services.UserCommandService;
import com.plantsync.platform.iam.domain.services.UserQueryService;
import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.commands.UpdateProfileCommand;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.services.ProfileCommandService;
import com.plantsync.platform.profiles.domain.services.ProfileQueryService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

  @Mock
  private UserCommandService userCommandService;

  @Mock
  private UserQueryService userQueryService;

  @Mock
  private ProfileCommandService profileCommandService;

  @Mock
  private ProfileQueryService profileQueryService;

  @Mock
  private Authentication authentication;

  @Mock
  private User user;

  @Mock
  private Profile profile;

  @InjectMocks
  private AccountController accountController;

  @Test
  void updateAccountShouldUpdateIamAndProfileKeepingCurrentSubscriptionPlan() {
    var resource = new UpdateAccountResource(
        "Jane",
        "Doe",
        "jane.doe@plantsync.com",
        "base64-profile-picture");

    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("old.email@plantsync.com");
    when(user.getId()).thenReturn(7L);
    when(profile.getId()).thenReturn(11L);
    when(profile.getSubscriptionPlan()).thenReturn(SubscriptionPlan.PREMIUM);
    when(userQueryService.handle(new GetUserByEmailQuery("old.email@plantsync.com")))
        .thenReturn(Optional.of(user));
    when(profileQueryService.handle(new GetProfileByUserIdQuery(7L)))
        .thenReturn(Optional.of(profile));
    when(userCommandService.handle(new UpdateUserCommand(7L, "jane.doe@plantsync.com")))
        .thenReturn(Optional.of(user));
    when(profileCommandService.handle(argThat(this::matchesExpectedProfileUpdate)))
        .thenReturn(Optional.of(profile));

    var response = accountController.updateAccount(resource, authentication);

    assertEquals(200, response.getStatusCode().value());
    verify(userCommandService).handle(new UpdateUserCommand(7L, "jane.doe@plantsync.com"));
    verify(profileCommandService).handle(argThat(this::matchesExpectedProfileUpdate));
  }

  private boolean matchesExpectedProfileUpdate(UpdateProfileCommand command) {
    return command.id().equals(11L)
        && command.personName().equals("Jane Doe")
        && command.subscriptionPlan().equals("PREMIUM")
        && command.profilePictureBase64().equals("base64-profile-picture");
  }
}
