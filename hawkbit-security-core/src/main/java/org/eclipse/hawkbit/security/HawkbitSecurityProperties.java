/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security related hawkBit configuration.
 */
@Data
@ConfigurationProperties("hawkbit.server.security")
public class HawkbitSecurityProperties {

    private final Clients clients = new Clients();
    private final Dos dos = new Dos();
    private final Cors cors = new Cors();

    /**
     * Secure access enforced.
     */
    private boolean requireSsl;
    /**
     * With this property a list of allowed hostnames can be configured. All
     * requests with different Host headers will be rejected.
     */
    private List<String> allowedHostNames;
    /**
     * Add paths that will be ignored by {@link org.springframework.security.web.firewall.StrictHttpFirewall}.
     */
    private List<String> httpFirewallIgnoredPaths;
    /**
     * Basic auth realm, see https://tools.ietf.org/html/rfc2617#page-3 .
     */
    private String basicRealm = "hawkBit";
    /**
     * If to allow http auth when there is OAuth2 auth enabled.
     */
    private boolean allowHttpBasicOnOAuthEnabled = false;

    /**
     * Security configuration related to CORS.
     */
    @Data
    public static class Cors {

        /**
         * Flag to enable CORS.
         */
        private boolean enabled = false;
        /**
         * Allowed origins for CORS.
         */
        private List<String> allowedOrigins = Collections.singletonList("http://localhost");
        /**
         * Allowed headers for CORS.
         */
        private List<String> allowedHeaders = Collections.singletonList("*");
        /**
         * Allowed methods for CORS.
         */
        private List<String> allowedMethods = Arrays.asList("DELETE", "GET", "POST", "PATCH", "PUT");
        /**
         * Exposed headers for CORS.
         */
        private List<String> exposedHeaders = Collections.emptyList();

        public CorsConfiguration toCorsConfiguration() {
            final CorsConfiguration corsConfiguration = new CorsConfiguration();

            corsConfiguration.setAllowedOrigins(getAllowedOrigins());
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.setAllowedHeaders(getAllowedHeaders());
            corsConfiguration.setAllowedMethods(getAllowedMethods());
            corsConfiguration.setExposedHeaders(getExposedHeaders());
            return corsConfiguration;
        }

        public CorsConfigurationSource toCorsConfigurationSource() {
            final CorsConfiguration corsConfiguration = toCorsConfiguration();
            return request -> corsConfiguration;
        }
    }

    /**
     * Security configuration related to clients.
     */
    @Data
    public static class Clients {

        public static final String X_FORWARDED_FOR = "X-Forwarded-For";
        /**
         * Blacklisted client (IP addresses) for for DDI and Management API.
         */
        private String blacklist = "";
        /**
         * Name of the http header from which the remote ip is extracted.
         */
        private String remoteIpHeader = X_FORWARDED_FOR;
        /**
         * Set to <code>true</code> if DDI clients remote IP should be stored.
         */
        private boolean trackRemoteIp = true;
    }

    /**
     * Denial of service protection related properties.
     */
    @Data
    public static class Dos {

        private final Filter filter = new Filter();
        private final Filter uiFilter = new Filter();
        /**
         * Maximum number of status updates that the controller can report for
         * an action (0 to disable).
         */
        private int maxStatusEntriesPerAction = 1000;
        /**
         * Maximum number of attributes that the controller can report;
         */
        private int maxAttributeEntriesPerTarget = 100;
        /**
         * Maximum number of allowed groups per Rollout.
         */
        private int maxRolloutGroupsPerRollout = 500;
        /**
         * Maximum number of messages per ActionStatus
         */
        private int maxMessagesPerActionStatus = 50;
        /**
         * Maximum number of meta data entries per software module
         */
        private int maxMetaDataEntriesPerSoftwareModule = 100;
        /**
         * Maximum number of meta data entries per distribution set
         */
        private int maxMetaDataEntriesPerDistributionSet = 100;
        /**
         * Maximum number of meta data entries per target
         */
        private int maxMetaDataEntriesPerTarget = 100;
        /**
         * Maximum number of software modules per distribution set
         */
        private int maxSoftwareModulesPerDistributionSet = 100;
        /**
         * Maximum number of software modules per distribution set
         */
        private int maxSoftwareModuleTypesPerDistributionSetType = 50;
        /**
         * Maximum number of artifacts per software module
         */
        private int maxArtifactsPerSoftwareModule = 50;
        /**
         * Maximum number of targets per rollout group
         */
        private int maxTargetsPerRolloutGroup = 20000;
        /**
         * Maximum number of overall actions targets per target
         */
        private int maxActionsPerTarget = 2000;
        /**
         * Maximum number of actions resulting from a manual assignment of
         * distribution sets and targets. Must be greater than 1000.
         */
        private int maxTargetDistributionSetAssignmentsPerManualAssignment = 5000;
        /**
         * Maximum number of targets for an automatic distribution set
         * assignment
         */
        private int maxTargetsPerAutoAssignment = 20000;
        /**
         * Maximum size of artifacts in bytes. Defaults to 1 GB.
         */
        private long maxArtifactSize = 1_073_741_824;
        /**
         * Maximum size of all artifacts in bytes. Defaults to 20 GB.
         */
        private long maxArtifactStorage = 21_474_836_480L;
        /**
         * Maximum number of distribution set types per target types
         */
        private int maxDistributionSetTypesPerTargetType = 50;

        /**
         * Configuration for hawkBits DOS prevention filter. This is usually an
         * infrastructure topic (e.g. Web Application Firewall (WAF)) but might
         * be useful in some cases, e.g. to prevent unintended misuse.
         */
        @Data
        public static class Filter {

            /**
             * True if filter is enabled.
             */
            private boolean enabled = true;
            /**
             * White list of peer IP addresses for DOS filter (regular
             * expression).
             */
            private String whitelist = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|169\\.254\\.\\d{1,3}\\.\\d{1,3}|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}";
            /**
             * # Maximum number of allowed REST read/GET requests per second per
             * client IP.
             */
            private int maxRead = 200;
            /**
             * Maximum number of allowed REST write/(PUT/POST/etc.) requests per
             * second per client IP.
             */
            private int maxWrite = 50;
        }
    }
}