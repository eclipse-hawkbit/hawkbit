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

import org.eclipse.hawkbit.mcp.server.client.AuthenticationValidator;
import org.eclipse.hawkbit.mcp.server.client.HawkbitAuthenticationValidator;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for authentication validation beans.
 */
@Configuration
public class McpValidationConfiguration {

    /**
     * Creates the hawkBit authentication validator when validation is enabled.
     * Validates credentials against hawkBit REST API with result caching.
     *
     * @param hawkbitClient the hawkBit client for API calls
     * @param dummyTenant the tenant configuration
     * @param properties the MCP properties with cache configuration
     * @return the authentication validator
     */
    @Bean
    @ConditionalOnProperty(name = "hawkbit.mcp.validation.enabled", havingValue = "true", matchIfMissing = true)
    public AuthenticationValidator hawkbitAuthenticationValidator(
            final HawkbitClient hawkbitClient,
            final Tenant dummyTenant,
            final HawkbitMcpProperties properties) {
        return new HawkbitAuthenticationValidator(hawkbitClient, dummyTenant, properties);
    }
}
