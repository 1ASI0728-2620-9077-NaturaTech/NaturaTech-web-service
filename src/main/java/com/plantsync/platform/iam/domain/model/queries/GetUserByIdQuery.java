package com.plantsync.platform.iam.domain.model.queries;

/**
 * Query to get a user by id.
 *
 * @param userId The user id.
 */
public record GetUserByIdQuery(Long userId) {
}
