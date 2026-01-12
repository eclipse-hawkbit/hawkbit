/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

import java.util.function.BiFunction;

/**
 * Configuration for the hawkBit SDK client.
 * <p>
 * Uses a custom request interceptor to inject authentication from the
 * current HTTP request context, enabling per-request credentials.
 * </p>
 */
@Slf4j
@Configuration
public class HawkBitClientConfiguration {

    @Bean
    @Primary
    public HawkbitServer hawkbitServer(final HawkBitMcpProperties properties) {
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
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String authHeader = (String) attrs.getAttribute(
                        McpSecurityConfiguration.AUTH_HEADER_ATTRIBUTE,
                        RequestAttributes.SCOPE_REQUEST);
                if (authHeader != null) {
                    template.header(HttpHeaders.AUTHORIZATION, authHeader);
                    log.trace("Injected auth header from request context");
                }
            }
        };
    }

    /**
     * Dummy tenant bean - actual authentication comes from request context via interceptor.
     */
    @Bean
    public Tenant dummyTenant() {
        Tenant tenant = new Tenant();
        tenant.setUsername(null);
        tenant.setPassword(null);
        return tenant;
    }
}
