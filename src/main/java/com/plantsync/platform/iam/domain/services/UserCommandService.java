package com.plantsync.platform.iam.domain.services;

import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.domain.model.commands.SignInCommand;
import com.plantsync.platform.iam.domain.model.commands.SignUpCommand;
import com.plantsync.platform.iam.domain.model.commands.UpdateUserCommand;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Service interface for handling user-related commands.
 *
 * <p>Provides methods for user authentication, registration, and updates.</p>
 */
public interface UserCommandService {
  /**
   * Handles the sign-in process.
   *
   * @param command The {@link SignInCommand}.
   * @return An {@link Optional} containing a pair of {@link User} and JWT token if successful.
   */
  Optional<ImmutablePair<User, String>> handle(SignInCommand command);

  /**
   * Handles the sign-up process.
   *
   * @param command The {@link SignUpCommand}.
   * @return An {@link Optional} containing the created {@link User} if successful.
   */
  Optional<User> handle(SignUpCommand command);

  /**
   * Handles the update of user information.
   *
   * @param command The {@link UpdateUserCommand}.
   * @return An {@link Optional} containing the updated {@link User} if successful.
   */
  Optional<User> handle(UpdateUserCommand command);

}
