package com.plantsync.platform.profiles.application.internal;

import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.plantsync.platform.profiles.domain.model.commands.UpdateProfileCommand;
import com.plantsync.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import com.plantsync.platform.profiles.domain.model.valueobjects.PaymentStatus;
import com.plantsync.platform.profiles.domain.model.valueobjects.PersonName;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import com.plantsync.platform.profiles.domain.services.ProfileCommandService;
import com.plantsync.platform.profiles.domain.services.ProfileQueryService;
import com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:plantsync-profiles-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;INIT=SET REFERENTIAL_INTEGRITY FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
class ProfilesIntegrationTest {

    @Autowired
    private ProfileCommandService profileCommandService;

    @Autowired
    private ProfileQueryService profileQueryService;

    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
    }

    @Test
    @Transactional
    void createProfileShouldPersistProfileAndAllowQueryById() {
        // Arrange
        CreateProfileCommand command = new CreateProfileCommand(
                new PersonName("John Doe"),
                SubscriptionPlan.BASIC,
                new UserId(100L));

        // Act
        profileCommandService.handle(command);

        List<Profile> allProfiles = profileRepository.findAll();
        assertFalse(allProfiles.isEmpty());

        Long createdProfileId = allProfiles.get(0).getId();
        Optional<Profile> queriedProfile = profileQueryService.handle(new GetProfileByIdQuery(createdProfileId));

        // Assert
        assertNotNull(createdProfileId);
        assertTrue(queriedProfile.isPresent());
        assertEquals("John Doe", queriedProfile.get().getPersonName().name());
        assertEquals(SubscriptionPlan.BASIC, queriedProfile.get().getSubscriptionPlan());
        assertEquals(100L, queriedProfile.get().getUserId().value());
        assertEquals(PaymentStatus.PENDING, queriedProfile.get().getPaymentStatus());
    }

    @Test
    @Transactional
    void updateProfileShouldModifyPersistedProfile() {
        // Arrange
        CreateProfileCommand createCommand = new CreateProfileCommand(
                new PersonName("Jane Doe"),
                SubscriptionPlan.BASIC,
                new UserId(101L));
        profileCommandService.handle(createCommand);

        Profile savedProfile = profileRepository.findAll().get(0);

        UpdateProfileCommand updateCommand = new UpdateProfileCommand(
                savedProfile.getId(),
                "Jane Smith",
                "PRO",
                "base64-profile-picture");

        // Act
        Optional<Profile> updatedProfileResult = profileCommandService.handle(updateCommand);

        // Assert
        assertTrue(updatedProfileResult.isPresent());
        assertEquals("Jane Smith", updatedProfileResult.get().getPersonName().name());
        assertEquals("base64-profile-picture", updatedProfileResult.get().getProfilePictureBase64());
        assertEquals(SubscriptionPlan.PRO, updatedProfileResult.get().getSubscriptionPlan());
        assertEquals(101L, updatedProfileResult.get().getUserId().value());
    }

    @Test
    @Transactional
    void getAllProfilesShouldReturnAllCreatedProfiles() {
        // Arrange
        CreateProfileCommand command1 = new CreateProfileCommand(
                new PersonName("User One"), SubscriptionPlan.BASIC, new UserId(1L));
        CreateProfileCommand command2 = new CreateProfileCommand(
                new PersonName("User Two"), SubscriptionPlan.PREMIUM, new UserId(2L));

        profileCommandService.handle(command1);
        profileCommandService.handle(command2);

        // Act
        List<Profile> profiles = profileQueryService.handle(new GetAllProfilesQuery());

        // Assert
        assertNotNull(profiles);
        assertEquals(2, profiles.size());

        assertTrue(profiles.stream().anyMatch(profile -> profile.getPersonName().name().equals("User One")));
        assertTrue(profiles.stream().anyMatch(profile -> profile.getPersonName().name().equals("User Two")));
    }
}
