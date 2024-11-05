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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        value = "hawkbit.server.swagger.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OpenApiConfiguration {

    private static final String API_TITLE = "hawkBit REST APIs";
    private static final String API_VERSION = "v1";
    private static final String DESCRIPTION = """
            Eclipse hawkBit™ is a domain-independent back-end framework for rolling out software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based networking infrastructure.
              """;

    private static final String BASIC_AUTH_SEC_SCHEME_NAME = "Basic Authentication";
    private static final String BEARER_AUTH_SEC_SCHEME_NAME = "Bearer Authentication";
    private static final String DDI_TOKEN_SEC_SCHEME_NAME = "DDI Target/GatewayToken Authentication";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info().title(API_TITLE).version(API_VERSION).description(DESCRIPTION));
    }

    @Bean
    @ConditionalOnProperty(
            value = "hawkbit.server.swagger.mgmt.api.group.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public GroupedOpenApi mgmtApi() {
        return GroupedOpenApi
                .builder()
                .group("Management API")
                .pathsToMatch("/rest/v1/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi
                            .addSecurityItem(new SecurityRequirement()
                                    .addList(BASIC_AUTH_SEC_SCHEME_NAME)
                                    .addList(BEARER_AUTH_SEC_SCHEME_NAME))
                            .components(
                                    openApi
                                            .getComponents()
                                            .addSecuritySchemes(BASIC_AUTH_SEC_SCHEME_NAME,
                                                    new SecurityScheme()
                                                            .name(BASIC_AUTH_SEC_SCHEME_NAME)
                                                            .type(SecurityScheme.Type.HTTP)
                                                            .in(SecurityScheme.In.HEADER)
                                                            .scheme("basic"))
                                            .addSecuritySchemes(BEARER_AUTH_SEC_SCHEME_NAME,
                                                    new SecurityScheme()
                                                            .name(BEARER_AUTH_SEC_SCHEME_NAME)
                                                            .type(SecurityScheme.Type.HTTP)
                                                            .in(SecurityScheme.In.HEADER)
                                                            .bearerFormat("JWT")
                                                            .scheme("bearer")));
                })
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            value = "hawkbit.server.swagger.ddi.api.group.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public GroupedOpenApi ddiApi() {
        return GroupedOpenApi
                .builder()
                .group("Direct Device Integration API")
                .pathsToMatch("/{tenant}/controller/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi
                            .addSecurityItem(new SecurityRequirement().addList(DDI_TOKEN_SEC_SCHEME_NAME))
                            .components(
                                    openApi
                                            .getComponents()
                                            .addSecuritySchemes(DDI_TOKEN_SEC_SCHEME_NAME,
                                                    new SecurityScheme()
                                                            .name("Authorization")
                                                            .type(SecurityScheme.Type.APIKEY)
                                                            .in(SecurityScheme.In.HEADER)
                                                            .description("Format: (Target|Gateway)Token &lt;token&gt;")));
                })
                .build();
    }
}
