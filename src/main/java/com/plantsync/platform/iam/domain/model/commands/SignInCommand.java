package com.plantsync.platform.iam.domain.model.commands;

/**
 * Command for signing in a user.
 *
 * <p>This record represents the data required to authenticate a user.</p>
 *
 * @param username The email of the user.
 * @param password The password of the user.
 */
public record SignInCommand(String username, String password) {
}