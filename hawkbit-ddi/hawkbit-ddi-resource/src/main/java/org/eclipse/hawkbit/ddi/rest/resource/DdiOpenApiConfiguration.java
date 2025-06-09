/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = OpenApi.HAWKBIT_SERVER_OPENAPI_ENABLED, havingValue = "true", matchIfMissing = true)
public class DdiOpenApiConfiguration {

    private static final String DDI_TOKEN_SEC_SCHEME_NAME = "Token";

    @Bean
    @ConditionalOnProperty(
            value = "hawkbit.server.openapi.ddi.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public GroupedOpenApi ddiApi() {
        return GroupedOpenApi
                .builder()
                .group("Direct Device Integration API")
                .pathsToMatch("/{tenant}/controller/**")
                .addOpenApiCustomizer(openApi ->
                        openApi
                                .info(new Info()
                                        .title("Direct Device Integration API")
                                        .version("v1")
                                        .description("The Direct Device Integration (DDI) API provides a way for devices to interact directly with the hawkBit."))
                                .addSecurityItem(new SecurityRequirement().addList(DDI_TOKEN_SEC_SCHEME_NAME))
                                .components(
                                        openApi
                                                .getComponents()
                                                .addSecuritySchemes(DDI_TOKEN_SEC_SCHEME_NAME,
                                                        new SecurityScheme()
                                                                .name(DDI_TOKEN_SEC_SCHEME_NAME)
                                                                .description("Format: (Target|Gateway)Token &lt;token&gt;")
                                                                .type(SecurityScheme.Type.APIKEY)
                                                                .in(SecurityScheme.In.HEADER))))
                .build();
    }
}