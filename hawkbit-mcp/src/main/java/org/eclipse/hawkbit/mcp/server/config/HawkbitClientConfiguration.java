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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Common configuration for the hawkBit SDK client.
 * <p>
 * Provides the {@link HawkbitServer} bean which is used by both HTTP and STDIO mode configurations.
 * Mode-specific beans (HawkbitClient, Tenant) are provided by:
 * <ul>
 *   <li>{@link McpHttpClientConfiguration} - for HTTP mode</li>
 *   <li>{@link McpStdioClientConfiguration} - for STDIO mode</li>
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
}
