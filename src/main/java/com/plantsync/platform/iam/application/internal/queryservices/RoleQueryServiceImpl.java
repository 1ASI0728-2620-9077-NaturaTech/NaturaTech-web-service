package com.plantsync.platform.iam.application.internal.queryservices;

import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.queries.GetAllRolesQuery;
import com.plantsync.platform.iam.domain.model.queries.GetRoleByNameQuery;
import com.plantsync.platform.iam.domain.services.RoleQueryService;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.RoleRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * RoleQueryServiceImpl class.
 * This class is used to handle the role queries.
 */
@Service
public class RoleQueryServiceImpl implements RoleQueryService {
  private final RoleRepository roleRepository;

  /**
   * RoleQueryServiceImpl constructor.
   *
   * @param roleRepository the role repository
   */
  public RoleQueryServiceImpl(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  /**
   * Handle the get all roles query.
   *
   * @param query the get all roles query
   * @return {@code List<Role>} the list of roles
   */
  @Override
  public List<Role> handle(GetAllRolesQuery query) {
    return roleRepository.findAll();
  }

  /**
   * Handle the get role by name query.
   *
   * @param query the get role by name query
   * @return {@code Optional<Role>} the role
   */
  @Override
  public Optional<Role> handle(GetRoleByNameQuery query) {
    return roleRepository.findByName(query.name());
  }
}