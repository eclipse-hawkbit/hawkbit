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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        value = OpenApiConfiguration.HAWKBIT_SERVER_SWAGGER_ENABLED,
        havingValue = "true",
        matchIfMissing = true)
public class OpenApiConfiguration {

    public static final String HAWKBIT_SERVER_SWAGGER_ENABLED = "hawkbit.server.swagger.enabled";

    private static final String API_TITLE = "hawkBit REST APIs";
    private static final String API_VERSION = "v1";
    private static final String DESCRIPTION = """
            Eclipse hawkBit™ is a domain-independent back-end framework for rolling out software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based networking infrastructure.
            """;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info().title(API_TITLE).version(API_VERSION).description(DESCRIPTION));
    }
}