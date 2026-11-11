package com.plantsync.platform.iam.interfaces.rest.resources;

/**
 * Auth User Resource.
 * Record with email, id and token.
 * */

public record AuthenticatedUserResource(Long id, String email, String token) {
}
