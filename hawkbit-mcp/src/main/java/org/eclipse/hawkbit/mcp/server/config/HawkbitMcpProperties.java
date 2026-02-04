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

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for the standalone hawkBit MCP server.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "hawkbit.mcp")
public class HawkbitMcpProperties {

    /**
     * Base URL of the hawkBit Management API (e.g., <a href="http://localhost:8080">...</a>).
     */
    @NotBlank(message = "hawkbit.mcp.mgmt-url must be configured")
    private String mgmtUrl;

    /**
     * Username for hawkBit authentication (used in STDIO mode).
     * Read directly from HAWKBIT_USERNAME environment variable.
     */
    @Value("${HAWKBIT_USERNAME:#{null}}")
    private String username;

    /**
     * Password for hawkBit authentication (used in STDIO mode).
     * Read directly from HAWKBIT_PASSWORD environment variable.
     */
    @Value("${HAWKBIT_PASSWORD:#{null}}")
    private String password;

    /**
     * Check if static credentials are configured.
     * Allows empty strings as valid values (for users who intentionally set empty password).
     */
    public boolean hasStaticCredentials() {
        return username != null && password != null;
    }

    /**
     * Whether to enable the built-in hawkBit tools.
     * Set to false to provide custom tool implementations.
     */
    private boolean toolsEnabled = true;

    /**
     * Whether to enable the built-in hawkBit documentation resources.
     * Set to false to provide custom resource implementations.
     */
    private boolean resourcesEnabled = true;

    /**
     * Whether to enable the built-in hawkBit prompts.
     * Set to false to provide custom prompt implementations.
     */
    private boolean promptsEnabled = true;

    /**
     * Authentication validation configuration.
     */
    private Validation validation = new Validation();

    /**
     * Operations configuration for enabling/disabling specific operations.
     */
    private Operations operations = new Operations();

    /**
     * Configuration for pre-authentication validation against hawkBit.
     */
    @Data
    public static class Validation {

        /**
         * Whether to validate authentication against hawkBit before processing MCP requests.
         */
        private boolean enabled = true;

        /**
         * Duration to cache authentication validation results.
         * Shorter values are more secure but increase load on hawkBit.
         */
        private Duration cacheTtl = Duration.ofSeconds(60);

        /**
         * Maximum number of entries in the authentication validation cache.
         */
        private int cacheMaxSize = 1000;
    }

    /**
     * Configuration for enabling/disabling operations at global and per-entity levels.
     */
    @Data
    public static class Operations {

        // Global defaults
        private boolean listEnabled = true;
        private boolean createEnabled = true;
        private boolean updateEnabled = true;
        private boolean deleteEnabled = true;

        // Per-entity overrides (null = use global)
        private EntityConfig targets = new EntityConfig();
        private RolloutConfig rollouts = new RolloutConfig();
        private EntityConfig distributionSets = new EntityConfig();
        private ActionConfig actions = new ActionConfig();
        private EntityConfig softwareModules = new EntityConfig();
        private EntityConfig targetFilters = new EntityConfig();

        /**
         * Check if an operation is enabled globally.
         */
        public boolean isGlobalOperationEnabled(final String operation) {
            return switch (operation.toLowerCase()) {
                case "list" -> listEnabled;
                case "create" -> createEnabled;
                case "update" -> updateEnabled;
                case "delete" -> deleteEnabled;
                default -> true;
            };
        }
    }

    /**
     * Per-entity operation configuration.
     */
    @Data
    public static class EntityConfig {

        private Boolean listEnabled;
        private Boolean createEnabled;
        private Boolean updateEnabled;
        private Boolean deleteEnabled;

        /**
         * Get the enabled state for an operation, or null if not set (use global).
         */
        public Boolean getOperationEnabled(final String operation) {
            return switch (operation.toLowerCase()) {
                case "list" -> listEnabled;
                case "create" -> createEnabled;
                case "update" -> updateEnabled;
                case "delete" -> deleteEnabled;
                default -> null;
            };
        }
    }

    /**
     * Rollout-specific operation configuration including lifecycle operations.
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class RolloutConfig extends EntityConfig {

        private Boolean startEnabled = true;
        private Boolean pauseEnabled = true;
        private Boolean stopEnabled = true;
        private Boolean resumeEnabled = true;
        private Boolean approveEnabled = true;
        private Boolean denyEnabled = true;
        private Boolean retryEnabled = true;
        private Boolean triggerNextGroupEnabled = true;

        @Override
        public Boolean getOperationEnabled(final String operation) {
            final Boolean baseResult = super.getOperationEnabled(operation);
            if (baseResult != null) {
                return baseResult;
            }
            return switch (operation.toLowerCase().replace("_", "-")) {
                case "start" -> startEnabled;
                case "pause" -> pauseEnabled;
                case "stop" -> stopEnabled;
                case "resume" -> resumeEnabled;
                case "approve" -> approveEnabled;
                case "deny" -> denyEnabled;
                case "retry" -> retryEnabled;
                case "trigger-next-group" -> triggerNextGroupEnabled;
                default -> null;
            };
        }
    }

    /**
     * Action-specific operation configuration.
     */
    @Data
    public static class ActionConfig {

        private Boolean listEnabled;
        private Boolean deleteEnabled;
        private Boolean deleteBatchEnabled = true;

        /**
         * Get the enabled state for an operation, or null if not set (use global).
         */
        public Boolean getOperationEnabled(final String operation) {
            return switch (operation.toLowerCase().replace("_", "-")) {
                case "list" -> listEnabled;
                case "delete" -> deleteEnabled;
                case "delete-batch" -> deleteBatchEnabled;
                default -> null;
            };
        }
    }
}
