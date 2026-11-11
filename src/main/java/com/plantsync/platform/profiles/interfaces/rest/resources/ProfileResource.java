package com.plantsync.platform.profiles.interfaces.rest.resources;

/**
 * Resource representation of a profile.
 *
 * @param id               The ID of the profile.
 * @param personName       The name of the person.
 * @param profilePictureBase64 The profile picture encoded as Base64.
 * @param subscriptionPlan The subscription plan.
 * @param userId           The ID of the associated user.
 */
public record ProfileResource(
    Long id,
    String personName,
    String profilePictureBase64,
    String subscriptionPlan,
    Long userId) {
}
