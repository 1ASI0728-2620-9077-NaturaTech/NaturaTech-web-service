package com.plantsync.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * The type Plant sync backend application.
 */
@SpringBootApplication
@EnableJpaAuditing
public class PlantSyncBackendApplication {

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(PlantSyncBackendApplication.class, args);
  }

}
