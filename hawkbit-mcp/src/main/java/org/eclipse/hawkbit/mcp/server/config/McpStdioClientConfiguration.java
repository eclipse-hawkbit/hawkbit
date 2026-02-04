/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.config;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.BiFunction;

/**
 * Configuration for STDIO mode.
 * <p>
 * In STDIO mode, authentication uses static credentials from configuration properties
 * (environment variables HAWKBIT_USERNAME and HAWKBIT_PASSWORD).
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.ai.mcp.server.stdio", havingValue = "true")
public class McpStdioClientConfiguration {

    private final HawkbitMcpProperties properties;

    /**
     * Request interceptor for STDIO mode - uses static credentials from configuration.
     */
    @Bean
    public BiFunction<Tenant, Controller, RequestInterceptor> hawkbitRequestInterceptor() {
        log.info("Configuring STDIO mode request interceptor (static credentials)");
        return (tenant, controller) -> template -> {
            if (properties.hasStaticCredentials()) {
                String credentials = properties.getUsername() + ":" + properties.getPassword();
                String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8));
                template.header(HttpHeaders.AUTHORIZATION, authHeader);
                log.trace("Using static credentials from properties (STDIO mode)");
            } else {
                log.warn("No static credentials configured for STDIO mode - request will likely fail");
            }
        };
    }

}
