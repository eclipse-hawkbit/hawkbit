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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.function.BiFunction;

/**
 * Configuration for HTTP mode.
 * <p>
 * In HTTP mode, authentication is extracted from the incoming HTTP request's
 * Authorization header and forwarded to hawkBit (per-request authentication).
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.ai.mcp.server.stdio", havingValue = "false", matchIfMissing = true)
public class McpHttpClientConfiguration {

    @Bean
    public HawkbitClient hawkbitClient(final HawkbitServer server,
                                       final Encoder encoder,
                                       final Decoder decoder,
                                       final Contract contract) {
        log.info("Configuring hawkBit client for HTTP mode (per-request authentication)");
        return HawkbitClient.builder()
                .hawkBitServer(server)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .requestInterceptorFn(httpRequestInterceptor())
                .build();
    }

    private BiFunction<Tenant, Controller, RequestInterceptor> httpRequestInterceptor() {
        return (tenant, controller) -> template -> {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String authHeader = (String) attrs.getAttribute(
                        McpSecurityConfiguration.AUTH_HEADER_ATTRIBUTE,
                        RequestAttributes.SCOPE_REQUEST);
                if (authHeader != null) {
                    template.header(HttpHeaders.AUTHORIZATION, authHeader);
                    log.trace("Using auth header from HTTP request context");
                } else {
                    log.warn("No authentication header in request context - request will likely fail");
                }
            } else {
                log.warn("No request context available - request will likely fail");
            }
        };
    }

    /**
     * Tenant bean for HTTP mode - credentials come from request context via interceptor.
     */
    @Bean
    public Tenant dummyTenant() {
        Tenant tenant = new Tenant();
        tenant.setUsername(null);
        tenant.setPassword(null);
        log.info("Configured tenant for HTTP mode (per-request authentication)");
        return tenant;
    }
}
