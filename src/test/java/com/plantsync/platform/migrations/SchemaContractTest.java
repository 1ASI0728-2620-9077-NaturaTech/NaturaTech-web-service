package com.plantsync.platform.migrations;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class SchemaContractTest {
  @Test
  void mysqlBaselineMatchesEntityMappings() throws Exception {
    Path output = Path.of("target/generated-mysql-schema.sql");
    Files.deleteIfExists(output);
    var configuration = new Configuration();
    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
    configuration.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
    configuration.setProperty(
        "hibernate.physical_naming_strategy",
        "com.plantsync.platform.shared.infrastructure.persistence.jpa.configuration.strategy.SnakeCaseWithPluralizedTablePhysicalNamingStrategy");
    configuration.setProperty("jakarta.persistence.schema-generation.database.action", "none");
    configuration.setProperty("jakarta.persistence.schema-generation.scripts.action", "create");
    configuration.setProperty(
        "jakarta.persistence.schema-generation.scripts.create-target", output.toString());
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.Entity.class));
    for (var bean : scanner.findCandidateComponents("com.plantsync.platform"))
      configuration.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
    try (var factory = configuration.buildSessionFactory()) {
      assertNotNull(factory);
    }
    assertTrue(Files.size(output) > 1000);
    if (!Boolean.getBoolean("schema.generate")) {
      var baseline =
          Files.readString(Path.of("src/main/resources/db/migration/V1__initial_schema.sql"));
      assertEquals(
          normalize(Files.readString(output)),
          normalize(baseline),
          "Entity schema changed: add a reviewed migration and update this contract test");
    }
  }

  private String normalize(String sql) {
    return sql.replaceAll("(?m)^--.*$", "").replaceAll("\\s+", " ").trim();
  }
}
