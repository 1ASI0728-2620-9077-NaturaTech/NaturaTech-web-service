package com.plantsync.platform.migrations;

import static org.junit.jupiter.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;

@EnabledIfSystemProperty(named = "mysql.it", matches = "true")
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "spring.flyway.enabled=true",
      "spring.flyway.baseline-on-migrate=false",
      "spring.flyway.clean-disabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"
    })
class MySqlMigrationTest {
  @Container
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.4").withDatabaseName("plantsync");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
  }

  @Autowired Flyway flyway;
  @Autowired JdbcTemplate jdbc;

  @Test
  void migratesFreshMySqlAndHibernateValidatesIt() {
    assertEquals(2, flyway.info().applied().length);
    assertEquals(0, flyway.migrate().migrationsExecuted);
    assertEquals(2, jdbc.queryForObject("select count(*) from roles", Integer.class));
    assertTrue(flyway.validateWithResult().validationSuccessful);
    assertTrue(
        jdbc.queryForObject(
                "select count(*) from information_schema.statistics where table_schema=database()"
                    + " and index_name='ix_readings_node_timestamp'",
                Integer.class)
            > 0);
  }

  @Test
  void refusesUnknownNonEmptySchemaInsteadOfAutomaticBaseline() {
    var unknown =
        Flyway.configure()
            .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
            .table("untracked_history")
            .baselineOnMigrate(false)
            .cleanDisabled(true)
            .load();
    assertThrows(FlywayException.class, unknown::migrate);
    assertEquals(2, flyway.info().applied().length);
  }

  @Test
  void protectsAgainstDestructiveClean() {
    assertThrows(FlywayException.class, flyway::clean);
  }

  @Test
  void checksumMismatchIsRejectedWithoutAutomaticRepair() {
    var original = jdbc.queryForObject("select checksum from flyway_schema_history where version='1'", Integer.class);
    try {
      jdbc.update("update flyway_schema_history set checksum=0 where version='1'");
      assertThrows(FlywayException.class, flyway::migrate);
      assertEquals(0, jdbc.queryForObject("select checksum from flyway_schema_history where version='1'", Integer.class));
    } finally {
      jdbc.update("update flyway_schema_history set checksum=? where version='1'", original);
    }
  }
}
