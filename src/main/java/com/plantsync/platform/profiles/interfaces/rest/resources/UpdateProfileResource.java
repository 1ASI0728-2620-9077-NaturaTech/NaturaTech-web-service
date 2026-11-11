package com.plantsync.platform.profiles.interfaces.rest.resources;

/**
 * The type Update profile resource.
 */
public record UpdateProfileResource(
    String personName,
    String subscriptionPlan,
    String profilePictureBase64
) {
}
