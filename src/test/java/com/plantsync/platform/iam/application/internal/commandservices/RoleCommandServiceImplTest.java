package com.plantsync.platform.iam.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plantsync.platform.iam.domain.model.commands.SeedRolesCommand;
import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.valueobjects.Roles;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleCommandServiceImplTest {

  @Mock private RoleRepository roleRepository;

  @InjectMocks private RoleCommandServiceImpl roleCommandService;

  @Test
  void handleSeedRolesCommandShouldSaveMissingRoles() {
    // Arrange
    var command = new SeedRolesCommand();
    when(roleRepository.existsByName(Roles.ROLE_USER)).thenReturn(false);
    when(roleRepository.existsByName(Roles.ROLE_ADMIN)).thenReturn(true);
    var roleCaptor = ArgumentCaptor.forClass(Role.class);

    // Act
    roleCommandService.handle(command);

    // Assert
    verify(roleRepository).existsByName(Roles.ROLE_USER);
    verify(roleRepository).save(roleCaptor.capture());
    assertEquals(Roles.ROLE_USER, roleCaptor.getValue().getName());
  }

  @Test
  void handleSeedRolesCommandShouldNotSaveExistingRoles() {
    // Arrange
    var command = new SeedRolesCommand();
    when(roleRepository.existsByName(Roles.ROLE_USER)).thenReturn(true);
    when(roleRepository.existsByName(Roles.ROLE_ADMIN)).thenReturn(true);

    // Act
    roleCommandService.handle(command);

    // Assert
    verify(roleRepository).existsByName(Roles.ROLE_USER);
    verify(roleRepository, never()).save(org.mockito.ArgumentMatchers.any(Role.class));
  }
}
