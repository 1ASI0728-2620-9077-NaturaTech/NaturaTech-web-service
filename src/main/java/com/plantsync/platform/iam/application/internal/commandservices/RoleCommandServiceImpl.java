package com.plantsync.platform.iam.application.internal.commandservices;

import com.plantsync.platform.iam.domain.model.commands.SeedRolesCommand;
import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.valueobjects.Roles;
import com.plantsync.platform.iam.domain.services.RoleCommandService;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.RoleRepository;
import java.util.Arrays;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link RoleCommandService} to handle {@link SeedRolesCommand}.
 */
@Service
public class RoleCommandServiceImpl implements RoleCommandService {

  private final RoleRepository roleRepository;

  /**
   * Constructor for RoleCommandServiceImpl.
   *
   * @param roleRepository The role repository.
   */
  public RoleCommandServiceImpl(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  /**
   * This method will handle the {@link SeedRolesCommand} and will create the roles if not exists.
   *
   * @param command {@link SeedRolesCommand}
   * @see SeedRolesCommand
   */
  @Override
  public void handle(SeedRolesCommand command) {
    Arrays.stream(Roles.values()).forEach(role -> {
      if (!roleRepository.existsByName(role)) {
        roleRepository.save(new Role(Roles.valueOf(role.name())));
      }
    });
  }
}
