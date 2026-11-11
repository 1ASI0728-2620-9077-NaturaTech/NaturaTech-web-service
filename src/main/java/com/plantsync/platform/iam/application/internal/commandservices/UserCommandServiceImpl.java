package com.plantsync.platform.iam.application.internal.commandservices;

import com.plantsync.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.plantsync.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.plantsync.platform.iam.domain.exceptions.RoleNotFoundException;
import com.plantsync.platform.iam.domain.exceptions.UserAlreadyExistsException;
import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.domain.model.commands.SignInCommand;
import com.plantsync.platform.iam.domain.model.commands.SignUpCommand;
import com.plantsync.platform.iam.domain.model.commands.UpdateUserCommand;
import com.plantsync.platform.iam.domain.services.UserCommandService;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.RoleRepository;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository;
import com.plantsync.platform.profiles.interfaces.acl.ProfilesContextFacade;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User command service implementation.
 *
 * <p>This class implements the {@link UserCommandService} interface and provides the implementation
 * for the {@link SignInCommand} and {@link SignUpCommand} commands.
 */
@Service
public class UserCommandServiceImpl implements UserCommandService {

  private final UserRepository userRepository;
  private final HashingService hashingService;
  private final TokenService tokenService;
  private final ProfilesContextFacade profilesContextFacade;
  private final RoleRepository roleRepository;

  /**
   * Constructor for UserCommandServiceImpl.
   *
   * @param userRepository The user repository.
   * @param hashingService The hashing service.
   * @param tokenService The token service.
   * @param profilesContextFacade The profiles context facade.
   * @param roleRepository The role repository.
   */
  public UserCommandServiceImpl(
      UserRepository userRepository,
      HashingService hashingService,
      TokenService tokenService,
      ProfilesContextFacade profilesContextFacade,
      RoleRepository roleRepository) {
    this.userRepository = userRepository;
    this.hashingService = hashingService;
    this.tokenService = tokenService;
    this.roleRepository = roleRepository;
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * Handle the sign-in command.
   *
   * <p>This method handles the {@link SignInCommand} command and returns the user and the token.
   *
   * @param command the sign-in command containing the email and password
   * @return and optional containing the user matching the email and the generated token
   * @throws RuntimeException if the user is not found or the password is invalid
   */
  @Override
  public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
    var user = userRepository.findByEmail(command.username());
    if (user.isEmpty()) {
      throw new BadCredentialsException("Invalid credentials");
    }
    if (!hashingService.matches(command.password(), user.get().getPassword())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    var token = tokenService.generateToken(user.get().getEmail());
    return Optional.of(ImmutablePair.of(user.get(), token));
  }

  /**
   * Handle the sign-up command.
   *
   * <p>This method handles the {@link SignUpCommand} command and returns the user.
   *
   * @param command the sign-up command containing the email and password
   * @return the created user
   */
  @Override
  @Transactional
  public Optional<User> handle(SignUpCommand command) {
    if (userRepository.existsByEmail(command.email())) {
      throw new UserAlreadyExistsException(command.email());
    }
    var roles =
        command.roles().stream()
            .map(
                role ->
                    roleRepository
                        .findByName(role.getName())
                        .orElseThrow(() -> new RoleNotFoundException(role.getName())))
            .toList();
    var user = new User(command.email(), hashingService.encode(command.password()), roles);
    userRepository.save(user);

    var createdUser = userRepository.findByEmail(command.email()).orElseThrow();

    profilesContextFacade.createProfile(
        command.name(), createdUser.getId(), command.subscriptionPlan());

    return Optional.of(createdUser);
  }

  @Override
  public Optional<User> handle(UpdateUserCommand command) {
    var optionalUser = userRepository.findById(command.id());
    if (optionalUser.isEmpty()) {
      return Optional.empty();
    }

    var user = optionalUser.get();

    return Optional.of(userRepository.save(user.updateInformation(command.email())));
  }
}
