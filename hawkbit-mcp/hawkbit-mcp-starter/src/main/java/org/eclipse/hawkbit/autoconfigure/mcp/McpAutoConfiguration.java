/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.mcp;

import org.eclipse.hawkbit.mcp.server.client.AuthenticationValidator;
import org.eclipse.hawkbit.mcp.server.client.HawkbitAuthenticationValidator;
import org.eclipse.hawkbit.mcp.server.config.HawkbitClientConfiguration;
import org.eclipse.hawkbit.mcp.server.config.HawkbitMcpProperties;
import org.eclipse.hawkbit.mcp.server.config.McpHttpClientConfiguration;
import org.eclipse.hawkbit.mcp.server.config.McpSecurityConfiguration;
import org.eclipse.hawkbit.mcp.server.config.McpStdioClientConfiguration;
import org.eclipse.hawkbit.mcp.server.config.McpToolConfiguration;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the hawkBit MCP server.
 */
@AutoConfiguration
@ConditionalOnClass(McpToolConfiguration.class)
@Import({
        McpToolConfiguration.class,
        HawkbitClientConfiguration.class,
        McpHttpClientConfiguration.class,
        McpStdioClientConfiguration.class,
        McpSecurityConfiguration.class
})
@EnableConfigurationProperties(HawkbitMcpProperties.class)
public class McpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "hawkbit.mcp.validation.enabled", havingValue = "true", matchIfMissing = true)
    public AuthenticationValidator hawkbitAuthenticationValidator(
            final HawkbitClient hawkbitClient, final Tenant dummyTenant, final HawkbitMcpProperties properties) {
        return new HawkbitAuthenticationValidator(hawkbitClient, dummyTenant, properties);
    }
}
