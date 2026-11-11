package com.plantsync.platform.profiles.application.internal.commandservices;

import com.plantsync.platform.profiles.domain.exceptions.ProfileUpdateException;
import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.plantsync.platform.profiles.domain.model.commands.UpdateProfileCommand;
import com.plantsync.platform.profiles.domain.model.valueobjects.PaymentStatus;
import com.plantsync.platform.profiles.domain.model.valueobjects.PersonName;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCommandServiceImplTest {

  @Mock
  private ProfileRepository profileRepository;

  @InjectMocks
  private ProfileCommandServiceImpl profileCommandService;

  @Test
  void handleCreateProfileCommandShouldSaveAndReturnProfile() {
    // Arrange
    var command = new CreateProfileCommand(
        new PersonName("Plant Owner"),
        SubscriptionPlan.BASIC,
        new UserId(1L)
    );
    var profileCaptor = ArgumentCaptor.forClass(Profile.class);

    // Act
    var result = profileCommandService.handle(command);

    // Assert
    assertTrue(result.isPresent());
    verify(profileRepository).save(profileCaptor.capture());
    assertSame(result.get(), profileCaptor.getValue());
    assertEquals(command.personName(), result.get().getPersonName());
    assertEquals(command.subscriptionPlan(), result.get().getSubscriptionPlan());
    assertEquals(command.userId(), result.get().getUserId());
    assertEquals(PaymentStatus.PENDING, result.get().getPaymentStatus());
  }

  @Test
  void handleUpdateProfileCommandShouldUpdateAndReturnProfileWhenItExists() {
    // Arrange
    var command = new UpdateProfileCommand(
        1L,
        "Updated Owner",
        "premium",
        "base64-profile-picture");
    var existingProfile = new Profile(
        new PersonName("Plant Owner"),
        SubscriptionPlan.BASIC,
        new UserId(1L),
        PaymentStatus.PENDING
    );
    when(profileRepository.findById(command.id())).thenReturn(Optional.of(existingProfile));
    when(profileRepository.save(existingProfile)).thenReturn(existingProfile);

    // Act
    var result = profileCommandService.handle(command);

    // Assert
    assertTrue(result.isPresent());
    assertSame(existingProfile, result.get());
    assertEquals(new PersonName(command.personName()), result.get().getPersonName());
    assertEquals(command.profilePictureBase64(), result.get().getProfilePictureBase64());
    assertEquals(SubscriptionPlan.PREMIUM, result.get().getSubscriptionPlan());
    verify(profileRepository).save(existingProfile);
  }

  @Test
  void handleUpdateProfileCommandShouldThrowWhenProfileDoesNotExist() {
    // Arrange
    var command = new UpdateProfileCommand(99L, "Updated Owner", "premium", "base64");
    when(profileRepository.findById(command.id())).thenReturn(Optional.empty());

    // Act
    var exception = assertThrows(IllegalArgumentException.class, () -> profileCommandService.handle(command));

    // Assert
    assertEquals("Profile is empty", exception.getMessage());
    verify(profileRepository, never()).save(any(Profile.class));
  }

  @Test
  void handleUpdateProfileCommandShouldWrapUpdateErrors() {
    // Arrange
    var command = new UpdateProfileCommand(1L, "Updated Owner", "invalid-plan", "base64");
    var existingProfile = new Profile(
        new PersonName("Plant Owner"),
        SubscriptionPlan.BASIC,
        new UserId(1L),
        PaymentStatus.PENDING
    );
    when(profileRepository.findById(command.id())).thenReturn(Optional.of(existingProfile));

    // Act
    var exception = assertThrows(ProfileUpdateException.class, () -> profileCommandService.handle(command));

    // Assert
    assertEquals("Error updating profile: No enum constant com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan.INVALID-PLAN", exception.getMessage());
    verify(profileRepository, never()).save(any(Profile.class));
  }
}
