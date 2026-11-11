package com.plantsync.platform.iam.domain.model.aggregates;

import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * User aggregate root. This class represents the aggregate root for the User entity.
 *
 * @see AuditableAbstractAggregateRoot
 */
@Getter
@Setter
@Entity
public class User extends AuditableAbstractAggregateRoot<User> {

  @NotBlank
  @Size(max = 254)
  @Column(unique = true)
  private String email;

  @NotBlank
  @Size(max = 120)
  private String password;

  @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles;

  /** Default constructor. */
  public User() {
    this.roles = new HashSet<>();
  }

  /**
   * Constructor with email and password.
   *
   * @param email The user email.
   * @param password The user password.
   */
  public User(String email, String password) {
    this.email = email;
    this.password = password;
    this.roles = new HashSet<>();
  }

  /**
   * Constructor with email, password, and roles.
   *
   * @param email The user email.
   * @param password The user password.
   * @param roles The user roles.
   */
  public User(String email, String password, List<Role> roles) {
    this(email, password);
    addRoles(roles);
  }

  /**
   * Add a role to the user.
   *
   * @param role the role to add
   * @return the user with the added role
   */
  public User addRole(Role role) {
    this.roles.add(role);
    return this;
  }

  /**
   * Add a list of roles to the user.
   *
   * @param roleList the list of roles to add
   * @return the user with the added roles
   */
  public User addRoles(List<Role> roleList) {
    var validatedRoleSet = Role.validateRoleSet(roleList);
    this.roles.addAll(validatedRoleSet);
    return this;
  }

  /**
   * Update user information.
   *
   * @param newEmail The new email.
   * @return The updated user.
   */
  public User updateInformation(String newEmail) {
    this.email = newEmail;
    return this;
  }
}
