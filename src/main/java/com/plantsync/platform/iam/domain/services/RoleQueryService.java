package com.plantsync.platform.iam.domain.services;

import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.queries.GetAllRolesQuery;
import com.plantsync.platform.iam.domain.model.queries.GetRoleByNameQuery;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for handling role-related queries.
 */
public interface RoleQueryService {
  /**
   * Handle get all roles query.
   *
   * @param query the {@link GetAllRolesQuery} query
   * @return a list of {@link Role} entities
   */
  List<Role> handle(GetAllRolesQuery query);

  /**
   * Handle get role by name query.
   *
   * @param query the {@link GetRoleByNameQuery} query
   * @return an {@link Optional} of {@link Role} entity
   */
  Optional<Role> handle(GetRoleByNameQuery query);
}