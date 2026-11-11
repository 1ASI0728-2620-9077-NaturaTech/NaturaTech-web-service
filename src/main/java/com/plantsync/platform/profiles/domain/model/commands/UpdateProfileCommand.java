package com.plantsync.platform.profiles.domain.model.commands;

/**
 * The type Update profile command.
 */
public record UpdateProfileCommand(
    Long id,
    String personName,
    String subscriptionPlan,
    String profilePictureBase64
) {
}
