package com.plantsync.platform.iam.application.internal.queryservices;

import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.queries.GetAllRolesQuery;
import com.plantsync.platform.iam.domain.model.queries.GetRoleByNameQuery;
import com.plantsync.platform.iam.domain.model.valueobjects.Roles;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleQueryServiceImplTest {

  @Mock
  private RoleRepository roleRepository;

  @InjectMocks
  private RoleQueryServiceImpl roleQueryService;

  @Test
  void handleGetAllRolesQueryShouldReturnAllRoles() {
    // Arrange
    var query = new GetAllRolesQuery();
    var roles = List.of(new Role(Roles.ROLE_USER));
    when(roleRepository.findAll()).thenReturn(roles);

    // Act
    var result = roleQueryService.handle(query);

    // Assert
    assertEquals(roles, result);
    verify(roleRepository).findAll();
  }

  @Test
  void handleGetRoleByNameQueryShouldReturnRoleWhenItExists() {
    // Arrange
    var query = new GetRoleByNameQuery(Roles.ROLE_USER);
    var role = new Role(Roles.ROLE_USER);
    when(roleRepository.findByName(query.name())).thenReturn(Optional.of(role));

    // Act
    var result = roleQueryService.handle(query);

    // Assert
    assertTrue(result.isPresent());
    assertSame(role, result.get());
    verify(roleRepository).findByName(query.name());
  }
}
