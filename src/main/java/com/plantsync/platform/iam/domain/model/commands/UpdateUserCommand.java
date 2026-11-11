package com.plantsync.platform.iam.domain.model.commands;

/**
 * The type Update user command.
 */
public record UpdateUserCommand(
    Long id,
    String email
) {
}
