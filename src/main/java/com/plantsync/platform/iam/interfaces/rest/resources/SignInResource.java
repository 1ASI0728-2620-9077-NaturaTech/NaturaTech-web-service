package com.plantsync.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * The type Sign in resource.
 */
public record SignInResource(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Password is required")
    String password

) {
}