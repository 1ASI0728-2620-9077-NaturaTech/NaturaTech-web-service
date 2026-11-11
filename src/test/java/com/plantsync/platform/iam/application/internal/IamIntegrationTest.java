package com.plantsync.platform.iam.application.internal;

import com.plantsync.platform.iam.domain.model.commands.SeedRolesCommand;
import com.plantsync.platform.iam.domain.model.commands.SignInCommand;
import com.plantsync.platform.iam.domain.model.commands.SignUpCommand;
import com.plantsync.platform.iam.domain.model.commands.UpdateUserCommand;
import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.plantsync.platform.iam.domain.model.queries.GetUserByEmailQuery;
import com.plantsync.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.plantsync.platform.iam.domain.model.valueobjects.Roles;
import com.plantsync.platform.iam.domain.services.RoleCommandService;
import com.plantsync.platform.iam.domain.services.UserCommandService;
import com.plantsync.platform.iam.domain.services.UserQueryService;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.RoleRepository;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository;
import com.plantsync.platform.profiles.interfaces.acl.ProfilesContextFacade;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:plantsync-iam-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false",
    "weather.api.key=test-weather-key",
    "authorization.jwt.secret=WriteHereYourSecretStringForTokenSigningCredentials",
    "authorization.jwt.expiration.days=7",
    "documentation.application.description=PlantSync Backend",
    "documentation.application.version=0.0.1-SNAPSHOT"
})
class IamIntegrationTest {

  @Autowired
  private RoleCommandService roleCommandService;

  @Autowired
  private UserCommandService userCommandService;

  @Autowired
  private UserQueryService userQueryService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @MockitoBean
  private ProfilesContextFacade profilesContextFacade;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    roleRepository.deleteAll();
    roleCommandService.handle(new SeedRolesCommand());
  }

  @Test
  @Transactional
  void signUpShouldPersistUserAndAllowQueryByEmailAndId() {
    // Arrange
    var command = signUpCommand("registration.flow@plantsync.com");
    when(profilesContextFacade.createProfile(eq(command.name()), anyLong(), eq(command.subscriptionPlan())))
        .thenReturn(1L);

    // Act
    var createdUser = userCommandService.handle(command).orElseThrow();
    var queriedByEmail = userQueryService.handle(new GetUserByEmailQuery(command.email())).orElseThrow();
    var queriedById = userQueryService.handle(new GetUserByIdQuery(createdUser.getId())).orElseThrow();

    // Assert
    assertNotNull(createdUser.getId());
    assertEquals(command.email(), queriedByEmail.getEmail());
    assertEquals(createdUser.getId(), queriedById.getId());
    assertTrue(queriedById.getRoles().stream().anyMatch(role -> role.getName() == Roles.ROLE_USER));
    verify(profilesContextFacade).createProfile(command.name(), createdUser.getId(), command.subscriptionPlan());
  }

  @Test
  @Transactional
  void signInShouldAuthenticatePersistedUserAndReturnToken() {
    // Arrange
    var password = "plain-password";
    var command = signUpCommand("login.flow@plantsync.com", password);
    when(profilesContextFacade.createProfile(eq(command.name()), anyLong(), eq(command.subscriptionPlan())))
        .thenReturn(1L);
    var createdUser = userCommandService.handle(command).orElseThrow();

    // Act
    var signInResult = userCommandService.handle(new SignInCommand(command.email(), password)).orElseThrow();

    // Assert
    assertEquals(createdUser.getId(), signInResult.getLeft().getId());
    assertEquals(command.email(), signInResult.getLeft().getEmail());
    assertNotNull(signInResult.getRight());
    assertNotEquals("", signInResult.getRight());
  }

  @Test
  @Transactional
  void updateUserShouldChangeEmailAndAllowQueryByNewEmail() {
    // Arrange
    var command = signUpCommand("update.flow@plantsync.com");
    var updatedEmail = "updated.flow@plantsync.com";
    when(profilesContextFacade.createProfile(eq(command.name()), anyLong(), eq(command.subscriptionPlan())))
        .thenReturn(1L);
    var createdUser = userCommandService.handle(command).orElseThrow();

    // Act
    var updatedUser = userCommandService.handle(new UpdateUserCommand(createdUser.getId(), updatedEmail)).orElseThrow();
    var queriedAfterUpdate = userQueryService.handle(new GetUserByEmailQuery(updatedEmail)).orElseThrow();

    // Assert
    assertEquals(updatedEmail, updatedUser.getEmail());
    assertEquals(createdUser.getId(), queriedAfterUpdate.getId());
    assertFalse(userQueryService.handle(new GetUserByEmailQuery(command.email())).isPresent());
  }

  @Test
  @Transactional
  void getAllUsersShouldIncludeUsersCreatedThroughCommands() {
    // Arrange
    var firstCommand = signUpCommand("list.first@plantsync.com");
    var secondCommand = signUpCommand("list.second@plantsync.com");
    when(profilesContextFacade.createProfile(eq(firstCommand.name()), anyLong(), eq(firstCommand.subscriptionPlan())))
        .thenReturn(1L);
    when(profilesContextFacade.createProfile(eq(secondCommand.name()), anyLong(), eq(secondCommand.subscriptionPlan())))
        .thenReturn(2L);
    var firstUser = userCommandService.handle(firstCommand).orElseThrow();
    var secondUser = userCommandService.handle(secondCommand).orElseThrow();

    // Act
    var users = userQueryService.handle(new GetAllUsersQuery());

    // Assert
    assertTrue(users.stream().anyMatch(user -> user.getId().equals(firstUser.getId())));
    assertTrue(users.stream().anyMatch(user -> user.getId().equals(secondUser.getId())));
    assertTrue(users.size() >= 2);
  }

  private SignUpCommand signUpCommand(String email) {
    return signUpCommand(email, "plain-password");
  }

  private SignUpCommand signUpCommand(String email, String password) {
    return new SignUpCommand(
        "Integration User",
        password,
        List.of(new Role(Roles.ROLE_USER)),
        email,
        "BASIC"
    );
  }
}
