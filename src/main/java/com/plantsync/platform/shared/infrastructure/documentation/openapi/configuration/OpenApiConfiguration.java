package com.plantsync.platform.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Open api configuration.
 */
@Configuration
public class OpenApiConfiguration {
  // Properties
  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${documentation.application.description}")
  private String applicationDescription;

  @Value("${documentation.application.version}")
  private String applicationVersion;

  // Methods

  /**
   * Plantsync platform open api open api.
   *
   * @return the open api
   */
  @Bean
  public OpenAPI plantsyncPlatformOpenApi() {
    // General configuration
    var openApi = new OpenAPI();
    openApi
        .info(new Info()
            .title(this.applicationName)
            .description(this.applicationDescription)
            .version(this.applicationVersion)
            .license(new License().name("Apache 2.0")
                .url("https://springdoc.org")))
        .externalDocs(new ExternalDocumentation()
            .description("PlantSync wiki Documentation")
        );

    // Add security schemes

    final String bearerSchemeName = "bearerAuth";
    final String edgeApiKeySchemeName = "EdgeApiKey";

    openApi
        .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
        .addSecurityItem(new SecurityRequirement().addList(edgeApiKeySchemeName))
        .components(new Components()
            .addSecuritySchemes(bearerSchemeName,
                new SecurityScheme()
                    .name(bearerSchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"))
            .addSecuritySchemes(edgeApiKeySchemeName,
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-Edge-Api-Key")));

    // Return the OpenAPI configuration object with all the settings

    return openApi;
  }
}