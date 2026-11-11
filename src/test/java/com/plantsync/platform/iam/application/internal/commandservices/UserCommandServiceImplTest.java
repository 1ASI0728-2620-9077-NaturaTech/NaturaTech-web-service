package com.plantsync.platform.iam.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plantsync.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.plantsync.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.plantsync.platform.iam.domain.exceptions.RoleNotFoundException;
import com.plantsync.platform.iam.domain.exceptions.UserAlreadyExistsException;
import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.domain.model.commands.SignInCommand;
import com.plantsync.platform.iam.domain.model.commands.SignUpCommand;
import com.plantsync.platform.iam.domain.model.commands.UpdateUserCommand;
import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.valueobjects.Roles;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.RoleRepository;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository;
import com.plantsync.platform.profiles.interfaces.acl.ProfilesContextFacade;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

  @Mock private UserRepository userRepository;

  @Mock private HashingService hashingService;

  @Mock private TokenService tokenService;

  @Mock private ProfilesContextFacade profilesContextFacade;

  @Mock private RoleRepository roleRepository;

  @InjectMocks private UserCommandServiceImpl userCommandService;

  @Test
  void handleSignInCommandShouldReturnAuthenticatedUserAndTokenWhenCredentialsAreValid() {
    // Arrange
    var command = new SignInCommand("user@plantsync.com", "plain-password");
    var user = new User("user@plantsync.com", "hashed-password");
    when(userRepository.findByEmail(command.username())).thenReturn(Optional.of(user));
    when(hashingService.matches(command.password(), user.getPassword())).thenReturn(true);
    when(tokenService.generateToken(user.getEmail())).thenReturn("jwt-token");

    // Act
    var result = userCommandService.handle(command);

    // Assert
    assertTrue(result.isPresent());
    assertSame(user, result.get().getLeft());
    assertEquals("jwt-token", result.get().getRight());
    verify(tokenService).generateToken(user.getEmail());
  }

  @Test
  void handleSignInCommandShouldThrowWhenUserDoesNotExist() {
    // Arrange
    var command = new SignInCommand("missing@plantsync.com", "plain-password");
    when(userRepository.findByEmail(command.username())).thenReturn(Optional.empty());

    // Act
    var exception = assertThrows(RuntimeException.class, () -> userCommandService.handle(command));

    // Assert
    assertEquals("Invalid credentials", exception.getMessage());
    verify(hashingService, never()).matches(any(), any());
    verify(tokenService, never()).generateToken(any());
  }

  @Test
  void handleSignInCommandShouldThrowWhenPasswordIsInvalid() {
    // Arrange
    var command = new SignInCommand("user@plantsync.com", "wrong-password");
    var user = new User("user@plantsync.com", "hashed-password");
    when(userRepository.findByEmail(command.username())).thenReturn(Optional.of(user));
    when(hashingService.matches(command.password(), user.getPassword())).thenReturn(false);

    // Act
    var exception = assertThrows(RuntimeException.class, () -> userCommandService.handle(command));

    // Assert
    assertEquals("Invalid credentials", exception.getMessage());
    verify(tokenService, never()).generateToken(any());
  }

  @Test
  void handleSignUpCommandShouldCreateUserAndProfileWhenEmailIsAvailable() {
    // Arrange
    var role = new Role(Roles.ROLE_USER);
    var command =
        new SignUpCommand(
            "Plant Owner", "plain-password", List.of(role), "owner@plantsync.com", "FREE");
    var createdUser = new User(command.email(), "encoded-password", List.of(role));
    var userCaptor = ArgumentCaptor.forClass(User.class);
    when(userRepository.existsByEmail(command.email())).thenReturn(false);
    when(roleRepository.findByName(Roles.ROLE_USER)).thenReturn(Optional.of(role));
    when(hashingService.encode(command.password())).thenReturn("encoded-password");
    when(userRepository.findByEmail(command.email())).thenReturn(Optional.of(createdUser));

    // Act
    var result = userCommandService.handle(command);

    // Assert
    assertTrue(result.isPresent());
    assertSame(createdUser, result.get());
    verify(userRepository).save(userCaptor.capture());
    assertEquals(command.email(), userCaptor.getValue().getEmail());
    assertEquals("encoded-password", userCaptor.getValue().getPassword());
    assertTrue(userCaptor.getValue().getRoles().contains(role));
    verify(profilesContextFacade).createProfile(command.name(), null, command.subscriptionPlan());
  }

  @Test
  void handleSignUpCommandShouldThrowWhenEmailAlreadyExists() {
    // Arrange
    var command =
        new SignUpCommand(
            "Plant Owner",
            "plain-password",
            List.of(new Role(Roles.ROLE_USER)),
            "owner@plantsync.com",
            "FREE");
    when(userRepository.existsByEmail(command.email())).thenReturn(true);

    // Act
    var exception =
        assertThrows(UserAlreadyExistsException.class, () -> userCommandService.handle(command));

    // Assert
    assertEquals("User with email 'owner@plantsync.com' already exists.", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
    verify(profilesContextFacade, never()).createProfile(any(), isNull(), any());
  }

  @Test
  void handleSignUpCommandShouldThrowWhenRoleDoesNotExist() {
    // Arrange
    var command =
        new SignUpCommand(
            "Plant Owner",
            "plain-password",
            List.of(new Role(Roles.ROLE_USER)),
            "owner@plantsync.com",
            "FREE");
    when(userRepository.existsByEmail(command.email())).thenReturn(false);
    when(roleRepository.findByName(Roles.ROLE_USER)).thenReturn(Optional.empty());

    // Act
    var exception =
        assertThrows(RoleNotFoundException.class, () -> userCommandService.handle(command));

    // Assert
    assertEquals("Role with name 'ROLE_USER' not found.", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
    verify(profilesContextFacade, never()).createProfile(any(), any(), any());
  }

  @Test
  void handleUpdateUserCommandShouldUpdateEmailWhenUserExists() {
    // Arrange
    var command = new UpdateUserCommand(1L, "updated@plantsync.com");
    var existingUser = new User("old@plantsync.com", "hashed-password");
    when(userRepository.findById(command.id())).thenReturn(Optional.of(existingUser));
    when(userRepository.save(existingUser)).thenReturn(existingUser);

    // Act
    var result = userCommandService.handle(command);

    // Assert
    assertTrue(result.isPresent());
    assertSame(existingUser, result.get());
    assertEquals(command.email(), result.get().getEmail());
    verify(userRepository).save(existingUser);
  }

  @Test
  void handleUpdateUserCommandShouldReturnEmptyWhenUserDoesNotExist() {
    // Arrange
    var command = new UpdateUserCommand(99L, "updated@plantsync.com");
    when(userRepository.findById(command.id())).thenReturn(Optional.empty());

    // Act
    var result = userCommandService.handle(command);

    // Assert
    assertTrue(result.isEmpty());
    verify(userRepository, never()).save(any(User.class));
  }
}
