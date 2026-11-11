package com.plantsync.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resource for signing up a user.
 *
 * @param name The name of the user.
 * @param email The email of the user.
 * @param password The password of the user.
 * @param subscriptionPlan The subscription plan of the user.
 */
public record SignUpResource(
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,
    @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,
    @NotNull(message = "Subscription plan is required") String subscriptionPlan) {}
