/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.eclipse.hawkbit.rest.OpenApiConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        value = "hawkbit.server.swagger.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MgmtOpenApiConfiguration {

    private static final String BASIC_AUTH_SEC_SCHEME_NAME = "Basic Authentication";
    private static final String BEARER_AUTH_SEC_SCHEME_NAME = "Bearer Authentication";

    @Bean
    @ConditionalOnProperty(
            value = OpenApiConfiguration.HAWKBIT_SERVER_SWAGGER_ENABLED,
            havingValue = "true",
            matchIfMissing = true)
    public GroupedOpenApi mgmtApi() {
        return GroupedOpenApi
                .builder()
                .group("Management API")
                .pathsToMatch("/rest/v*/**")
                .addOpenApiCustomizer(openApi ->
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
                                                                .scheme("bearer")))
                                .tags(OpenApiConfiguration.sort(openApi.getTags())))
                .build();
    }
}