package com.plantsync.platform.iam.infrastructure.authorization.sfs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.plantsync.platform.iam.domain.model.aggregates.User;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Implementation of the UserDetails interface for Spring Security.
 */
@Getter
@EqualsAndHashCode
public class UserDetailsImpl implements UserDetails {

  private final String email;
  @JsonIgnore
  private final String password;
  private final boolean accountNonExpired;
  private final boolean accountNonLocked;
  private final boolean credentialsNonExpired;
  private final boolean enabled;
  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * Constructor for UserDetailsImpl.
   *
   * @param email       The email of the user.
   * @param password    The password of the user.
   * @param authorities The collection of granted authorities.
   */
  public UserDetailsImpl(String email, String password,
                         Collection<? extends GrantedAuthority> authorities) {
    this.email = email;
    this.password = password;
    this.authorities = authorities;
    this.accountNonExpired = true;
    this.accountNonLocked = true;
    this.credentialsNonExpired = true;
    this.enabled = true;
  }

  /**
   * Builds a UserDetailsImpl object from a User entity.
   *
   * @param user The {@link User} entity.
   * @return A {@link UserDetailsImpl} instance.
   */
  public static UserDetailsImpl build(User user) {
    var authorities = user.getRoles().stream()
        .map(role -> role.getName().name())
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
    return new UserDetailsImpl(
        user.getEmail(),
        user.getPassword(),
        authorities);
  }

  @Override
  public String getUsername() {
    return email;
  }

}
