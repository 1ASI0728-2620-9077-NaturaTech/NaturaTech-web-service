package com.plantsync.platform.profiles.application.internal.queryservices;

import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.plantsync.platform.profiles.domain.model.valueobjects.PaymentStatus;
import com.plantsync.platform.profiles.domain.model.valueobjects.PersonName;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileQueryServiceImplTest {

  @Mock
  private ProfileRepository profileRepository;

  @InjectMocks
  private ProfileQueryServiceImpl profileQueryService;

  @Test
  void handleGetProfileByIdQueryShouldReturnProfileWhenItExists() {
    // Arrange
    var query = new GetProfileByIdQuery(1L);
    var profile = createProfile();
    when(profileRepository.findById(query.profileId())).thenReturn(Optional.of(profile));

    // Act
    var result = profileQueryService.handle(query);

    // Assert
    assertTrue(result.isPresent());
    assertSame(profile, result.get());
    verify(profileRepository).findById(query.profileId());
  }

  @Test
  void handleGetAllProfilesQueryShouldReturnAllProfiles() {
    // Arrange
    var query = new GetAllProfilesQuery();
    var profiles = List.of(createProfile());
    when(profileRepository.findAll()).thenReturn(profiles);

    // Act
    var result = profileQueryService.handle(query);

    // Assert
    assertEquals(profiles, result);
    verify(profileRepository).findAll();
  }

  @Test
  void handleGetProfileByUserIdQueryShouldReturnProfileUsingQueryUserId() {
    // Arrange
    var query = new GetProfileByUserIdQuery(1L);
    var profile = createProfile();
    var userId = new UserId(query.userId());
    when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

    // Act
    var result = profileQueryService.handle(query);

    // Assert
    assertTrue(result.isPresent());
    assertSame(profile, result.get());
    verify(profileRepository).findByUserId(userId);
  }

  private Profile createProfile() {
    return new Profile(
        new PersonName("Plant Owner"),
        SubscriptionPlan.BASIC,
        new UserId(1L),
        PaymentStatus.PENDING
    );
  }
}
