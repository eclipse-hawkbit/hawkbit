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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.BiFunction;

/**
 * Configuration for the hawkBit SDK client.
 * <p>
 * Uses a custom request interceptor to inject authentication from the
 * current HTTP request context (HTTP mode) or from static credentials (STDIO mode).
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
    public HawkbitClient hawkbitClient(final HawkbitServer server,
                                       final Encoder encoder,
                                       final Decoder decoder,
                                       final Contract contract) {
        return HawkbitClient.builder()
                .hawkBitServer(server)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .requestInterceptorFn(requestContextInterceptor())
                .build();
    }

    private BiFunction<Tenant, Controller, RequestInterceptor> requestContextInterceptor() {
        return (tenant, controller) -> template -> {
            String authHeader = null;

            // Try request context first (HTTP mode)
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                authHeader = (String) attrs.getAttribute(
                        McpSecurityConfiguration.AUTH_HEADER_ATTRIBUTE,
                        RequestAttributes.SCOPE_REQUEST);
                if (authHeader != null) {
                    log.trace("Using auth header from HTTP request context");
                }
            }

            // Fall back to static credentials (STDIO mode)
            if (authHeader == null && properties.hasStaticCredentials()) {
                String credentials = properties.getUsername() + ":" + properties.getPassword();
                authHeader = "Basic " + Base64.getEncoder().encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8));
                log.trace("Using static credentials from properties (STDIO mode)");
            }

            // Apply header if available
            if (authHeader != null) {
                template.header(HttpHeaders.AUTHORIZATION, authHeader);
            } else {
                log.warn("No authentication available - request will likely fail");
            }
        };
    }

    /**
     * Tenant bean - uses static credentials if configured (STDIO mode),
     * otherwise actual authentication comes from request context via interceptor (HTTP mode).
     */
    @Bean
    public Tenant dummyTenant() {
        Tenant tenant = new Tenant();
        if (properties.hasStaticCredentials()) {
            tenant.setUsername(properties.getUsername());
            tenant.setPassword(properties.getPassword());
            log.info("Configured tenant with static credentials (STDIO mode)");
        } else {
            tenant.setUsername(null);
            tenant.setPassword(null);
            log.info("Configured tenant without static credentials (HTTP mode - per-request auth)");
        }
        return tenant;
    }
}
