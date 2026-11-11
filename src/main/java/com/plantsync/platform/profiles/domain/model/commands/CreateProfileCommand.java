package com.plantsync.platform.profiles.domain.model.commands;

import com.plantsync.platform.profiles.domain.model.valueobjects.PersonName;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;

/**
 * Command used to create a new  Profile
 * Contains the necessary data required to instantiate a new profile.
 *
 * @param personName       the full name of the user creating the profile
 * @param subscriptionPlan the selected subscription plan (e.g., FREE, PRO)
 * @param userId           the ID of the user associated with the profile
 */
public record CreateProfileCommand(
    PersonName personName,
    SubscriptionPlan subscriptionPlan,
    UserId userId


) {
}
