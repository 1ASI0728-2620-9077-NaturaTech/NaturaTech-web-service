package com.plantsync.platform.iam.infrastructure.authorization.sfs.services;

import com.plantsync.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for providing the user details to the Spring Security framework.
 * It implements the UserDetailsService interface.
 */
@Service(value = "defaultUserDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Instantiates a new User details service.
   *
   * @param userRepository the user repository
   */
  public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * This method is responsible for loading the user details from the database.
   *
   * @param email The email.
   * @return The UserDetails object.
   * @throws UsernameNotFoundException If the user is not found.
   */
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    var user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    return UserDetailsImpl.build(user);
  }
}
