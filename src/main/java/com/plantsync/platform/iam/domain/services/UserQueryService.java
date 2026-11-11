package com.plantsync.platform.iam.domain.services;

import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.plantsync.platform.iam.domain.model.queries.GetUserByEmailQuery;
import com.plantsync.platform.iam.domain.model.queries.GetUserByIdQuery;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for handling user-related queries.
 *
 * <p>Provides methods to retrieve user information by different criteria.</p>
 */
public interface UserQueryService {
  /**
   * Handles the retrieval of all users.
   *
   * @param query The {@link GetAllUsersQuery}.
   * @return A list of {@link User} entities.
   */
  List<User> handle(GetAllUsersQuery query);

  /**
   * Handles the retrieval of a user by their ID.
   *
   * @param query The {@link GetUserByIdQuery}.
   * @return An {@link Optional} containing the {@link User} if found.
   */
  Optional<User> handle(GetUserByIdQuery query);

  /**
   * Handles the retrieval of a user by their email.
   *
   * @param query The {@link GetUserByEmailQuery}.
   * @return An {@link Optional} containing the {@link User} if found.
   */
  Optional<User> handle(GetUserByEmailQuery query);

}
