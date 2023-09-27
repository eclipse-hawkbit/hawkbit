/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

  private static final String DESCRIPTION = """
      Eclipse hawkBit™ is a domain-independent back-end framework for rolling out software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based networking infrastructure.
        """;

  @Bean
  public OpenAPI customOpenApi() {
    final String apiTitle = "hawkBit REST APIs";

    final String basiAuthSecSchemeName = "basicAuth";
    final String bearerAuthenticationSchemeName = "Bearer Authentication"; // check for ddi

    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList(basiAuthSecSchemeName))
        .addSecurityItem(new SecurityRequirement().addList(bearerAuthenticationSchemeName))
        .components(
            new Components()
                .addSecuritySchemes(basiAuthSecSchemeName,
                    new SecurityScheme()
                        .name(basiAuthSecSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .in(SecurityScheme.In.HEADER)
                        .scheme("basic")
                )
                .addSecuritySchemes(bearerAuthenticationSchemeName,
                    new SecurityScheme()
                        .name(bearerAuthenticationSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .bearerFormat("JWT")
                        .scheme("bearer")))
        .info(new Info().title(apiTitle).description(DESCRIPTION).version("v1"));
  }

  @Bean
  @ConditionalOnProperty(
      value="hawkbit.server.swagger.mgmt.api.group.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public GroupedOpenApi mgmtApi() {
    return GroupedOpenApi.builder()
        .group("Management API")
        .pathsToMatch("/rest/v1/**")
        .build();
  }

  @Bean
  @ConditionalOnProperty(
      value="hawkbit.server.swagger.ddi.api.group.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public GroupedOpenApi ddiApi() {
    return GroupedOpenApi.builder()
        .group("Direct Device Integration API")
        .pathsToMatch("/{tenant}/controller/**")
        .build();
  }
}
