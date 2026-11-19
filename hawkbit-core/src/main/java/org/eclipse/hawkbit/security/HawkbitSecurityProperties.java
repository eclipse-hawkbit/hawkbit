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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Forwarded forwarded = new Forwarded();
    private final Dos dos = new Dos();
    private final Cors cors = new Cors();
    private final TenantResolver tenantResolver = new TenantResolver();

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
     * Basic authentication realm, see https://tools.ietf.org/html/rfc2617#page-3 .
     */
    private String basicRealm = "hawkBit";
    /**
     * If to allow http authentication when there is OAuth2 authentication enabled.
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
     * Management API requested tenant resolution - the tenant requested before the authentication (see
     * <code>org.eclipse.hawkbit.rest.security.TenantResolverFilter</code>).
     */
    @Data
    public static class TenantResolver {

        /**
         * If <code>true</code>, <code>/{tenant}/rest/**</code> is served as <code>/rest/**</code> with requested tenant
         * <code>tenant</code>. Disable it only if the tenant path is handled otherwise (e.g. by extension controllers mapped under
         * <code>/{tenant}/rest/**</code>).
         */
        private boolean tenantPath = true;
        /**
         * If <code>true</code>, a global, not tenant scoped, user (e.g. <code>spring.security.user</code>) acts in the scope of the
         * requested tenant. Otherwise, the global user stays not tenant scoped.
         */
        private boolean globalUserRequestedTenant = false;
        /**
         * If <code>true</code>, a <code>tenant&#92;user</code> basic auth user on <code>/rest/**</code> requests the <code>tenant</code>
         * and authenticates as <code>user</code> (as with the <code>/{tenant}/rest/**</code> path).
         *
         * @deprecated since 1.2.1 - use the tenant path <code>/{tenant}/rest/**</code> to request a tenant. Will be removed.
         */
        @Deprecated(forRemoval = true, since = "1.2.1")
        @SuppressWarnings("java:S1133")
        private boolean tenantPrefixedBasicAuth = true;
    }

    /**
     * Security configuration related to clients.
     */
    @Data
    public static class Clients {

        public static final String X_FORWARDED_FOR = "X-Forwarded-For";
        /**
         * Blacklisted client (IP addresses) for DDI and Management API.
         */
        private String blacklist = "";
        /**
         * Header with the client IP chain (comma separated, the proxies append the address they got the request from). In legacy mode
         * (forward headers strategy other than <code>NONE</code>) the client IP is extracted from it as is.
         */
        private String remoteIpHeader = X_FORWARDED_FOR;
        /**
         * Set to <code>true</code> if DDI clients remote IP should be stored.
         */
        private boolean trackRemoteIp = true;
    }

    /**
     * Forwarded headers processing by hawkBit - applied only with <code>server.forward-headers-strategy=NONE</code> (the default), otherwise
     * the client IP is read as is from {@link Clients#getRemoteIpHeader()} (legacy). The connection peer has to be a trusted proxy in
     * order its forwarded headers to be applied.
     */
    @Data
    public static class Forwarded {

        /**
         * Trusted proxies - a comma separated list of IPs / CIDR blocks. If not set, the private, loopback, link-local and CGNAT address
         * ranges (IPv4 and IPv6). Set, but empty - nothing trusted. The client IP chain is read from {@link Clients#getRemoteIpHeader()}.
         */
        private String trustedProxies;
        /**
         * Header with the original protocol (scheme). Applied only if the connection peer is trusted.
         */
        private String protocolHeader = "X-Forwarded-Proto";
        /**
         * Header with the original host. Applied only if the connection peer is trusted.
         */
        private String hostHeader = "X-Forwarded-Host";
        /**
         * Header with the original port. Applied only if the connection peer is trusted.
         */
        private String portHeader = "X-Forwarded-Port";
        /**
         * Per tenant trusted proxies (the key is the tenant, case-insensitive) - a comma separated list of IPs / CIDR blocks, e.g. a customer
         * proxy of the tenant's devices. Applied for the DDI requests of the tenant (<code>/{tenant}/controller/**</code>) coming through
         * one of them: the hops left to it are trusted only if they are trusted proxies of the tenant.
         */
        private Map<String, String> tenants = new HashMap<>();
    }

    /**
     * Denial-of-service (DoS) protection related properties.
     */
    @Data
    public static class Dos {

        private final Filter filter = new Filter();

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
         * Maximum number of allowed distinct target groups.
         */
        private int maxTargetGroups = 100;
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
             * White list of client IP addresses for DOS filter (regular expression, found in the IP - anchor it to match the whole IP).
             */
            private String whitelist = "^(?:10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|169\\.254\\.\\d{1,3}\\.\\d{1,3}|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3})$";
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
            /**
             * Maximum number of failed authentications (HTTP 401) per second per client IP. Above it, the requests of the client IP are
             * rejected (HTTP 429) without authentication. Counted separately from the read / write requests. 0 to disable.
             */
            private int maxAuthFailures = 10;

            /**
             * Management API (<code>/rest/**</code>) per client IP limits.
             */
            private final Limit mgmt = new Limit();
            /**
             * DDI API (<code>/{tenant}/controller/v1/**</code>) per client IP limits.
             */
            private final Limit ddi = new Limit();
            /**
             * DDI artifact download per client IP limits.
             */
            private final Limit ddiDownload = new Limit();
            /**
             * System API (<code>/system/*</code>) per client IP limits.
             */
            private final Limit system = new Limit();
            /**
             * Load report threshold - a client IP reaching this percent of its read or write limit is logged (once per report interval, the
             * first time immediately) and reported with its peak rates at the end of the report interval - e.g. to find proxies to trust.
             * 0 to disable.
             */
            private int reportPercent = 20;
            /**
             * Load report interval.
             */
            private Duration reportInterval = Duration.ofHours(1);

            /**
             * @return the effective max read requests per second of the given limits - {@link #getMaxRead()} if not set
             */
            public int maxRead(final Limit limit) {
                return limit.getMaxRead() == null ? maxRead : limit.getMaxRead();
            }

            /**
             * @return the effective max write requests per second of the given limits - {@link #getMaxWrite()} if not set
             */
            public int maxWrite(final Limit limit) {
                return limit.getMaxWrite() == null ? maxWrite : limit.getMaxWrite();
            }

            /**
             * Request limits per second. Not set (<code>null</code>) - the global ones ({@link Filter#getMaxRead()},
             * {@link Filter#getMaxWrite()}).
             */
            @Data
            public static class Limit {

                /**
                 * Maximum number of read (GET) requests per second.
                 */
                private Integer maxRead;
                /**
                 * Maximum number of write (PUT/POST/etc.) requests per second.
                 */
                private Integer maxWrite;
            }
        }
    }
}
