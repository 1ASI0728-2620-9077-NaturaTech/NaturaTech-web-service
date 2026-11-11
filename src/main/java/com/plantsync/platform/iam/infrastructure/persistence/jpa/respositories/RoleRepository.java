package com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories;

import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.valueobjects.Roles;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Role} entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  /**
   * Finds a role by its name.
   *
   * @param name The name of the role.
   * @return An {@link Optional} containing the role if found.
   */
  Optional<Role> findByName(Roles name);

  /**
   * Checks if a role with the given name exists.
   *
   * @param name The name of the role.
   * @return True if it exists, false otherwise.
   */
  boolean existsByName(Roles name);
}
