package com.plantsync.platform.iam.domain.model.commands;

import com.plantsync.platform.iam.domain.model.entities.Role;
import java.util.List;

/**
 * Command for signing up a user.
 *
 * <p>This record represents the data required to register a new user.</p>
 *
 * @param name             The name of the user.
 * @param password         The password of the user.
 * @param roles            The roles of the user.
 * @param email            The email of the user.
 * @param subscriptionPlan The subscription plan of the user.
 */
public record SignUpCommand(String name, String password, List<Role> roles,
                            String email, String subscriptionPlan) {
}