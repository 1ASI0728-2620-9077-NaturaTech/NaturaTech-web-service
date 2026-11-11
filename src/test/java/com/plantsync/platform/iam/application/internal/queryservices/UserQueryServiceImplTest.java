package com.plantsync.platform.iam.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.plantsync.platform.iam.domain.model.queries.GetUserByEmailQuery;
import com.plantsync.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserQueryServiceImpl userQueryService;

  @Test
  void handleGetAllUsersQueryShouldReturnAllUsers() {
    // Arrange
    var query = new GetAllUsersQuery();
    var users = List.of(new User("owner@plantsync.com", "hashed-password"));
    when(userRepository.findAll()).thenReturn(users);

    // Act
    var result = userQueryService.handle(query);

    // Assert
    assertEquals(users, result);
    verify(userRepository).findAll();
  }

  @Test
  void handleGetUserByIdQueryShouldReturnUserWhenItExists() {
    // Arrange
    var query = new GetUserByIdQuery(1L);
    var user = new User("owner@plantsync.com", "hashed-password");
    when(userRepository.findById(query.userId())).thenReturn(Optional.of(user));

    // Act
    var result = userQueryService.handle(query);

    // Assert
    assertTrue(result.isPresent());
    assertSame(user, result.get());
    verify(userRepository).findById(query.userId());
  }

  @Test
  void handleGetUserByEmailQueryShouldReturnUserWhenItExists() {
    // Arrange
    var query = new GetUserByEmailQuery("owner@plantsync.com");
    var user = new User(query.email(), "hashed-password");
    when(userRepository.findByEmail(query.email())).thenReturn(Optional.of(user));

    // Act
    var result = userQueryService.handle(query);

    // Assert
    assertTrue(result.isPresent());
    assertSame(user, result.get());
    verify(userRepository).findByEmail(query.email());
  }
}
