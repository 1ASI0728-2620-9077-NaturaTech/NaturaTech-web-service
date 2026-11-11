package com.plantsync.platform.iam.interfaces.rest.resources;

import java.util.List;

/**
 * The type User resource.
 */
public record UserResource(Long id, String email, List<String> roles) {
}