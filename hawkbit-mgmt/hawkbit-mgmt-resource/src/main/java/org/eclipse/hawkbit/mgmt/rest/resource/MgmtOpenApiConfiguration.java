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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.oas.models.tags.Tag;
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
            value = "hawkbit.server.openapi.mgmt.enabled", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi mgmtApi(
            @Value("${hawkbit.server.openapi.mgmt.tenant-endpoint.enabled:false}") final boolean tenantEndpointEnabled,
            @Value("${hawkbit.server.security.oauth2.resourceserver.enabled:false}") final boolean oauth2Enabled,
            @Value("${hawkbit.server.security.allow-http-basic-on-o-auth-enabled:false}") final boolean allowBasicAuthWithOauth,
            @Value("${springdoc.oauth-flow.authorizationUrl:}") final String authorizationUrl,
            @Value("${springdoc.oauth-flow.tokenUrl:}") final String tokenUrl
    ) {
        // @formatter:off
        Map<String, SecurityScheme> securitySchemeMap = new HashMap<>();
        final SecurityRequirement securityRequirement = new SecurityRequirement();
        if (!oauth2Enabled || allowBasicAuthWithOauth) {
            securityRequirement.addList(BASIC_AUTH_SEC_SCHEME_NAME);
            securitySchemeMap.put(BASIC_AUTH_SEC_SCHEME_NAME,
                    new SecurityScheme()
                            .description(BASIC_AUTH_SEC_SCHEME_NAME + " Authentication")
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("basic"));
        }
        if(oauth2Enabled){
            securityRequirement.addList(BEARER_AUTH_SEC_SCHEME_NAME);
            securitySchemeMap.put(BEARER_AUTH_SEC_SCHEME_NAME,
                    new SecurityScheme()
                            .description(BEARER_AUTH_SEC_SCHEME_NAME + " Authentication")
                            .type(SecurityScheme.Type.OAUTH2)
                            .flows(new OAuthFlows()
                                    .authorizationCode(new OAuthFlow().authorizationUrl(authorizationUrl).tokenUrl(tokenUrl))
                                    .clientCredentials(new OAuthFlow().tokenUrl(tokenUrl)))
                            .bearerFormat("JWT")
                            .scheme("bearer"));
        }
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
                                .addSecurityItem(securityRequirement)
                                .components(
                                        openApi
                                                .getComponents()
                                                .securitySchemes(securitySchemeMap))
                                .tags(sort(openApi.getTags())))
                .build();
        // @formatter:on
    }

    private static ServerVariable tenantSeverVariable() {
        final ServerVariable tenantServerVariable = new ServerVariable();
        tenantServerVariable.setDescription("AccessContext identifier");
        tenantServerVariable.setDefault("DEFAULT");
        return tenantServerVariable;
    }

    private static final String ORDER = "order";
    private static final Comparator<Tag> TAG_COMPARATOR = new Comparator<>() {

        @Override
        public int compare(final Tag o1, final Tag o2) {
            final int o1Order = order(o1);
            final int o2Order = order(o2);
            if (o1Order == o2Order) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return Integer.compare(o1Order, o2Order);
            }
        }

        private static int order(final Tag tag) {
            return Optional.ofNullable(tag.getExtensions())
                    .map(extensions -> extensions.get(OpenApi.X_HAWKBIT))
                    .filter(extension -> Map.class.isAssignableFrom(extension.getClass()))
                    .map(Map.class::cast)
                    .map(propertiesMap -> propertiesMap.get(ORDER))
                    .map(String.class::cast)
                    .map(Integer::parseInt)
                    .orElse(0);
        }
    };

    private static List<Tag> sort(final List<Tag> tags) {
        tags.sort(TAG_COMPARATOR);
        return tags;
    }
}