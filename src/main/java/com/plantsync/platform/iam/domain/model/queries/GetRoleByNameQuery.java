package com.plantsync.platform.iam.domain.model.queries;

import com.plantsync.platform.iam.domain.model.valueobjects.Roles;

/**
 * The type Get role by name query.
 */
public record GetRoleByNameQuery(Roles name) {
}
