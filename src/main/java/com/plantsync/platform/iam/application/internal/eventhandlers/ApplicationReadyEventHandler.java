package com.plantsync.platform.iam.application.internal.eventhandlers;

import com.plantsync.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.domain.model.commands.SeedRolesCommand;
import com.plantsync.platform.iam.domain.model.valueobjects.Roles;
import com.plantsync.platform.iam.domain.services.RoleCommandService;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.RoleRepository;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository;
import java.sql.Timestamp;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ApplicationReadyEventHandler class. This class is used to handle the ApplicationReadyEvent. */
@Service
public class ApplicationReadyEventHandler {
  private final RoleCommandService roleCommandService;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final HashingService hashingService;
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReadyEventHandler.class);

  /** ApplicationReadyEventHnadler class. This class is used to call command role service. */
  public ApplicationReadyEventHandler(
      RoleCommandService roleCommandService,
      UserRepository userRepository,
      RoleRepository roleRepository,
      HashingService hashingService) {
    this.roleCommandService = roleCommandService;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.hashingService = hashingService;
  }

  /**
   * Handle the ApplicationReadyEvent. This method is used to seed the roles.
   *
   * @param event the ApplicationReadyEvent the event to handle
   */
  @EventListener
  @Transactional
  public void on(ApplicationReadyEvent event) {
    var applicationName = event.getApplicationContext().getId();
    LOGGER.info(
        "Starting to verify if roles seeding is needed for {} at {}",
        applicationName,
        currentTimestamp());
    var seedRolesCommand = new SeedRolesCommand();
    roleCommandService.handle(seedRolesCommand);
    LOGGER.info(
        "Roles seeding verification finished for {} at {}", applicationName, currentTimestamp());

    LOGGER.info(
        "Starting to verify if admin user seeding is needed for {} at {}",
        applicationName,
        currentTimestamp());
    seedAdminUser();
    LOGGER.info(
        "Admin user seeding verification finished for {} at {}",
        applicationName,
        currentTimestamp());
  }

  @org.springframework.beans.factory.annotation.Value("${bootstrap.admin.email:}")
  private String adminEmail;

  @org.springframework.beans.factory.annotation.Value("${bootstrap.admin.password:}")
  private String adminPassword;

  private void seedAdminUser() {
    if (adminEmail.isBlank() && adminPassword.isBlank()) return;
    if (!adminEmail.contains("@") || adminPassword.length() < 16) {
      throw new IllegalStateException(
          "Bootstrap admin requires an email and password of at least 16 characters");
    }
    if (!userRepository.existsByEmail(adminEmail)) {
      var role = roleRepository.findByName(Roles.ROLE_ADMIN).orElseThrow();
      userRepository.save(
          new User(adminEmail, hashingService.encode(adminPassword), List.of(role)));
      LOGGER.info("Configured administrator initialized");
    }
  }

  private Timestamp currentTimestamp() {
    return new Timestamp(System.currentTimeMillis());
  }
}
