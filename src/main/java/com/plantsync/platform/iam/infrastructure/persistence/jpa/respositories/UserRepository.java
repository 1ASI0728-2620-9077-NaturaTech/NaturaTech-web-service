package com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories;

import com.plantsync.platform.iam.domain.model.aggregates.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface User repository.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  /**
   * This method is responsible for finding the user by email.
   *
   * @param email The email.
   * @return The user object.
   */
  Optional<User> findByEmail(String email);

  /**
   * This method is responsible for checking if the user exists by email.
   *
   * @param email The email.
   * @return True if the user exists, false otherwise.
   */
  boolean existsByEmail(String email);

}
