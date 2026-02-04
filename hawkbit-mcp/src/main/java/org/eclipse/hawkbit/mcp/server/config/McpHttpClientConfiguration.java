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
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
@ConditionalOnProperty(name = "spring.ai.mcp.server.stdio", havingValue = "false", matchIfMissing = true)
public class McpHttpClientConfiguration {

    /**
     * Request interceptor for HTTP mode - extracts authentication from incoming HTTP request.
     */
    @Bean
    public BiFunction<Tenant, Controller, RequestInterceptor> hawkbitRequestInterceptor() {
        log.info("Configuring HTTP mode request interceptor (per-request authentication)");
        return (tenant, controller) -> template -> {
            final ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                final HttpServletRequest request = attrs.getRequest();
                final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null) {
                    template.header(HttpHeaders.AUTHORIZATION, authHeader);
                    log.trace("Using auth header from HTTP request");
                } else {
                    log.warn("No authentication header in request - request will likely fail");
                }
            } else {
                log.warn("No request context available - request will likely fail");
            }
        };
    }

}
