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
import org.eclipse.hawkbit.mcp.server.client.NoOpAuthenticationValidator;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for authentication validation beans.
 * Creates either a real validator (with caching) or a no-op validator based on configuration.
 */
@Configuration
public class McpValidationConfiguration {

    /**
     * Creates the hawkBit authentication validator when validation is enabled.
     * It validates against hawkBit REST API.
     */
    @Bean
    @ConditionalOnProperty(name = "hawkbit.mcp.validation.enabled", havingValue = "true", matchIfMissing = true)
    public AuthenticationValidator hawkbitAuthenticationValidator(
            final HawkbitClient hawkbitClient,
            final Tenant dummyTenant,
            final HawkbitMcpProperties properties) {
        return new HawkbitAuthenticationValidator(hawkbitClient, dummyTenant, properties);
    }

    /**
     * Creates a no-op authentication validator when validation is disabled.
     * This validator always returns VALID without any actual validation.
     */
    @Bean
    @ConditionalOnProperty(name = "hawkbit.mcp.validation.enabled", havingValue = "false")
    public AuthenticationValidator noOpAuthenticationValidator() {
        return new NoOpAuthenticationValidator();
    }
}
