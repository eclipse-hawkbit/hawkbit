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

import java.util.List;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = OpenApi.HAWKBIT_SERVER_OPENAPI_ENABLED, havingValue = "true", matchIfMissing = true)
public class MgmtOpenApiConfiguration {

    private static final String BASIC_AUTH_SEC_SCHEME_NAME = "Basic";
    private static final String BEARER_AUTH_SEC_SCHEME_NAME = "Bearer";

    @Bean
    @ConditionalOnProperty(
            value = "hawkbit.server.openapi.mgmt.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public GroupedOpenApi mgmtApi(@Value("${hawkbit.server.openapi.mgmt.tenant-endpoint.enabled:false}") final boolean tenantEndpointEnabled) {
        return GroupedOpenApi
                .builder()
                .group("Management API")
                .pathsToMatch(tenantEndpointEnabled ? new String[] { "/rest/v*/**", "/{tenant}/rest/v*/**" } : new String[] { "/rest/v*/**" })
                .addOpenApiCustomizer(openApi ->
                        openApi
                                .info(new Info()
                                        .title("Management API")
                                        .version("v1")
                                        .description("""
                                                The Management API provides access to the management features of the hawkBit.
                                                It allows for managing devices, deployments, and other.
                                                """))
                                .servers(tenantEndpointEnabled
                                        ? List.of(
                                            new Server()
                                                    .url("/{tenant}/")
                                                    .variables(new ServerVariables().addServerVariable("tenant", tenantSeverVariable())),
                                            new Server().url("/"))
                                        : List.of(new Server().url("/")))
                                .addSecurityItem(new SecurityRequirement()
                                        .addList(BASIC_AUTH_SEC_SCHEME_NAME)
                                        .addList(BEARER_AUTH_SEC_SCHEME_NAME))
                                .components(
                                        openApi
                                                .getComponents()
                                                .addSecuritySchemes(BASIC_AUTH_SEC_SCHEME_NAME,
                                                        new SecurityScheme()
                                                                .description(BASIC_AUTH_SEC_SCHEME_NAME + " Authentication")
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("basic"))
                                                .addSecuritySchemes(BEARER_AUTH_SEC_SCHEME_NAME,
                                                        new SecurityScheme()
                                                                .description(BEARER_AUTH_SEC_SCHEME_NAME + " Authentication")
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .bearerFormat("JWT")
                                                                .scheme("bearer")))
                                .tags(OpenApi.sort(openApi.getTags())))
                .build();
    }

    private static ServerVariable tenantSeverVariable() {
        final ServerVariable tenantServerVariable = new ServerVariable();
        tenantServerVariable.setDescription("AccessContext identifier");
        tenantServerVariable.setDefault("DEFAULT");
        return tenantServerVariable;
    }
}