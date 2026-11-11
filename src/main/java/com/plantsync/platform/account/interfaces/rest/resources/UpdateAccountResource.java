package com.plantsync.platform.account.interfaces.rest.resources;

/**
 * Resource used to update the authenticated user's account data.
 */
public record UpdateAccountResource(
    String firstName,
    String lastName,
    String email,
    String profilePictureBase64
) {
}
