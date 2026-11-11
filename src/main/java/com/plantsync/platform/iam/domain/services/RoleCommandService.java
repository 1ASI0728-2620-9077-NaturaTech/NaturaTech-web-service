package com.plantsync.platform.iam.domain.services;

import com.plantsync.platform.iam.domain.model.commands.SeedRolesCommand;

/**
 * The interface Role command service.
 */
public interface RoleCommandService {
  /**
   * Handle seed roles command.
   *
   * @param command the {@link SeedRolesCommand} command.
   */
  void handle(SeedRolesCommand command);
}
