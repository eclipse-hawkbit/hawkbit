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

import org.eclipse.hawkbit.mcp.server.prompts.HawkBitPromptProvider;
import org.eclipse.hawkbit.mcp.server.resources.HawkBitDocumentationResource;
import org.eclipse.hawkbit.mcp.server.tools.HawkBitMcpToolProvider;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MCP tools, resources, and prompts.
 * <p>
 * <ul>
 *   <li>All beans use {@code @ConditionalOnMissingBean} - override by defining your own bean</li>
 *   <li>Properties allow disabling built-in tools/resources/prompts</li>
 *   <li>Spring AI MCP auto-discovers {@code @Tool}, {@code @McpResource}, and {@code @McpPrompt} annotations</li>
 * </ul>
 * </p>
 */
@Configuration
public class McpToolConfiguration {

    /**
     * Creates the hawkBit tool provider.
     * <p>
     * Spring AI MCP auto-discovers {@code @McpTool} annotated methods on this bean.
     * Override by defining your own {@code HawkBitMcpToolProvider} bean.
     * Disable by setting {@code hawkbit.mcp.tools-enabled=false}.
     * Individual operations can be enabled/disabled via {@code hawkbit.mcp.operations.*} properties.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "hawkbit.mcp.tools-enabled", havingValue = "true", matchIfMissing = true)
    public HawkBitMcpToolProvider hawkBitMcpToolProvider(
            final HawkbitClient hawkbitClient,
            final Tenant dummyTenant,
            final HawkBitMcpProperties properties) {
        return new HawkBitMcpToolProvider(hawkbitClient, dummyTenant, properties);
    }

    /**
     * Creates the hawkBit documentation resource provider.
     * <p>
     * Spring AI MCP auto-discovers {@code @McpResource} annotated methods on this bean.
     * Override by defining your own {@code HawkBitDocumentationResource} bean.
     * Disable by setting {@code hawkbit.mcp.resources-enabled=false}.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "hawkbit.mcp.resources-enabled", havingValue = "true", matchIfMissing = true)
    public HawkBitDocumentationResource hawkBitDocumentationResource() {
        return new HawkBitDocumentationResource();
    }

    /**
     * Creates the hawkBit prompt provider.
     * <p>
     * Spring AI MCP auto-discovers {@code @McpPrompt} annotated methods on this bean.
     * Override by defining your own {@code HawkBitPromptProvider} bean.
     * Disable by setting {@code hawkbit.mcp.prompts-enabled=false}.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "hawkbit.mcp.prompts-enabled", havingValue = "true", matchIfMissing = true)
    public HawkBitPromptProvider hawkBitPromptProvider() {
        return new HawkBitPromptProvider();
    }
}
