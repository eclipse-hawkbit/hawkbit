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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.function.BiFunction;

/**
 * Common configuration for the hawkBit SDK client.
 * <p>
 * Provides the {@link HawkbitServer} and {@link HawkbitClient} beans.
 * Mode-specific authentication is provided via the request interceptor bean from:
 * <ul>
 *   <li>{@link McpHttpClientConfiguration} - for HTTP mode (per-request authentication)</li>
 *   <li>{@link McpStdioClientConfiguration} - for STDIO mode (static credentials)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HawkbitClientConfiguration {

    private final HawkbitMcpProperties properties;

    @Bean
    @Primary
    public HawkbitServer hawkbitServer() {
        HawkbitServer server = new HawkbitServer();
        server.setMgmtUrl(properties.getMgmtUrl());
        log.info("Configured hawkBit server URL: {}", properties.getMgmtUrl());
        return server;
    }

    @Bean
    public HawkbitClient hawkbitClient(
            final HawkbitServer server,
            final Encoder encoder,
            final Decoder decoder,
            final Contract contract,
            final BiFunction<Tenant, Controller, RequestInterceptor> hawkbitRequestInterceptor) {
        log.info("Configuring hawkBit client");
        return HawkbitClient.builder()
                .hawkBitServer(server)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .requestInterceptorFn(hawkbitRequestInterceptor)
                .build();
    }

    /**
     * Tenant bean - credentials depend on the mode:
     * <ul>
     *   <li>STDIO mode with credentials: uses static credentials from configuration</li>
     *   <li>STDIO mode without credentials: null credentials (will fail)</li>
     *   <li>HTTP mode: null credentials (auth comes from request context via interceptor)</li>
     * </ul>
     */
    @Bean
    public Tenant dummyTenant(@Value("${spring.ai.mcp.server.stdio:false}") final boolean stdioMode) {
        final Tenant tenant = new Tenant();
        // Tenant fields are null by default - only set credentials in STDIO mode when configured
        if (stdioMode) {
            if (properties.hasStaticCredentials()) {
                tenant.setUsername(properties.getUsername());
                tenant.setPassword(properties.getPassword());
                log.info("Configured tenant with static credentials for STDIO mode");
            } else {
                log.warn("STDIO mode enabled but no static credentials configured");
            }
        } else {
            log.info("Configured tenant for HTTP mode (per-request authentication)");
        }
        return tenant;
    }
}
