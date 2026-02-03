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

import feign.Contract;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitServer;
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

    @Bean
    public HawkbitClient hawkbitClient(final HawkbitServer server,
                                       final Encoder encoder,
                                       final Decoder decoder,
                                       final Contract contract) {
        log.info("Configuring hawkBit client for STDIO mode (static credentials)");
        return HawkbitClient.builder()
                .hawkBitServer(server)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .requestInterceptorFn(stdioRequestInterceptor())
                .build();
    }

    private BiFunction<Tenant, Controller, RequestInterceptor> stdioRequestInterceptor() {
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

    /**
     * Tenant bean for STDIO mode - uses static credentials from configuration.
     */
    @Bean
    public Tenant dummyTenant() {
        Tenant tenant = new Tenant();
        if (properties.hasStaticCredentials()) {
            tenant.setUsername(properties.getUsername());
            tenant.setPassword(properties.getPassword());
            log.info("Configured tenant with static credentials for STDIO mode");
        } else {
            tenant.setUsername(null);
            tenant.setPassword(null);
            log.warn("STDIO mode enabled but no static credentials configured");
        }
        return tenant;
    }
}
