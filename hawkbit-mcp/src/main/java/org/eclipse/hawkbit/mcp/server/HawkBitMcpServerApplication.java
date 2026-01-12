/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server;

import org.eclipse.hawkbit.mcp.server.config.HawkBitMcpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Standalone MCP Server application that connects to hawkBit via REST API.
 * <p>
 * This server acts as a proxy between MCP clients and hawkBit,
 * passing through authentication credentials to the hawkBit REST API.
 * </p>
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(HawkBitMcpProperties.class)
public class HawkBitMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HawkBitMcpServerApplication.class, args);
    }
}
