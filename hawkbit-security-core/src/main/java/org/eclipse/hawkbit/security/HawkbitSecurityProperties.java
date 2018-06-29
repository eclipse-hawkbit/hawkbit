/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security related hawkbit configuration.
 *
 */
@ConfigurationProperties("hawkbit.server.security")
public class HawkbitSecurityProperties {

    private final Clients clients = new Clients();
    private final Dos dos = new Dos();

    /**
     * Content Security policy Header for Manager UI.
     */
    private String contentSecurityPolicy;

    public String getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    public void setContentSecurityPolicy(final String contentSecurityPolicy) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    public Dos getDos() {
        return dos;
    }

    public Clients getClients() {
        return clients;
    }

    /**
     * Security configuration related to clients.
     *
     */
    public static class Clients {

        /**
         * Blacklisted client (IP addresses) for for DDI and Management API.
         */
        private String blacklist = "";

        /**
         * Name of the http header from which the remote ip is extracted.
         */
        private String remoteIpHeader = "X-Forwarded-For";

        /**
         * Set to <code>true</code> if DDI clients remote IP should be stored.
         */
        private boolean trackRemoteIp = true;

        public String getBlacklist() {
            return blacklist;
        }

        public void setBlacklist(final String blacklist) {
            this.blacklist = blacklist;
        }

        public String getRemoteIpHeader() {
            return remoteIpHeader;
        }

        public void setRemoteIpHeader(final String remoteIpHeader) {
            this.remoteIpHeader = remoteIpHeader;
        }

        public boolean isTrackRemoteIp() {
            return trackRemoteIp;
        }

        public void setTrackRemoteIp(final boolean trackRemoteIp) {
            this.trackRemoteIp = trackRemoteIp;
        }
    }

    /**
     * Denial of service protection related properties.
     *
     */
    public static class Dos {

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
         * Maximum number of targets per rollout group
         */
        private int maxActionsPerTarget = 2000;

        /**
         * Maximum number of targets for a manual distribution set assignment.
         * Must be greater than 1000.
         */
        private int maxTargetsPerManualAssignment = 5000;

        /**
         * Maximum number of targets for an automatic distribution set
         * assignment
         */
        private int maxTargetsPerAutoAssignment = 5000;

        /**
         * Maximum size of artifacts in bytes.
         */
        private long maxArtifactSize = 1_000_000_000;

        private final Filter filter = new Filter();
        private final Filter uiFilter = new Filter();

        public Filter getUiFilter() {
            return uiFilter;
        }

        public Filter getFilter() {
            return filter;
        }

        public int getMaxStatusEntriesPerAction() {
            return maxStatusEntriesPerAction;
        }

        public void setMaxStatusEntriesPerAction(final int maxStatusEntriesPerAction) {
            this.maxStatusEntriesPerAction = maxStatusEntriesPerAction;
        }

        public int getMaxMessagesPerActionStatus() {
            return maxMessagesPerActionStatus;
        }

        public void setMaxMessagesPerActionStatus(final int maxMessagesPerActionStatus) {
            this.maxMessagesPerActionStatus = maxMessagesPerActionStatus;
        }

        public int getMaxAttributeEntriesPerTarget() {
            return maxAttributeEntriesPerTarget;
        }

        public void setMaxAttributeEntriesPerTarget(final int maxAttributeEntriesPerTarget) {
            this.maxAttributeEntriesPerTarget = maxAttributeEntriesPerTarget;
        }

        public int getMaxRolloutGroupsPerRollout() {
            return maxRolloutGroupsPerRollout;
        }

        public void setMaxRolloutGroupsPerRollout(final int maxRolloutGroupsPerRollout) {
            this.maxRolloutGroupsPerRollout = maxRolloutGroupsPerRollout;
        }

        public int getMaxMetaDataEntriesPerSoftwareModule() {
            return maxMetaDataEntriesPerSoftwareModule;
        }

        public void setMaxMetaDataEntriesPerSoftwareModule(final int maxMetaDataEntriesPerSoftwareModule) {
            this.maxMetaDataEntriesPerSoftwareModule = maxMetaDataEntriesPerSoftwareModule;
        }

        public int getMaxMetaDataEntriesPerDistributionSet() {
            return maxMetaDataEntriesPerDistributionSet;
        }

        public void setMaxMetaDataEntriesPerDistributionSet(final int maxMetaDataEntriesPerDistributionSet) {
            this.maxMetaDataEntriesPerDistributionSet = maxMetaDataEntriesPerDistributionSet;
        }

        public int getMaxSoftwareModulesPerDistributionSet() {
            return maxSoftwareModulesPerDistributionSet;
        }

        public void setMaxSoftwareModulesPerDistributionSet(final int maxSoftwareModulesPerDistributionSet) {
            this.maxSoftwareModulesPerDistributionSet = maxSoftwareModulesPerDistributionSet;
        }

        public int getMaxSoftwareModuleTypesPerDistributionSetType() {
            return maxSoftwareModuleTypesPerDistributionSetType;
        }

        public void setMaxSoftwareModuleTypesPerDistributionSetType(
                final int maxSoftwareModuleTypesPerDistributionSetType) {
            this.maxSoftwareModuleTypesPerDistributionSetType = maxSoftwareModuleTypesPerDistributionSetType;
        }

        public int getMaxArtifactsPerSoftwareModule() {
            return maxArtifactsPerSoftwareModule;
        }

        public void setMaxArtifactsPerSoftwareModule(final int maxArtifactsPerSoftwareModule) {
            this.maxArtifactsPerSoftwareModule = maxArtifactsPerSoftwareModule;
        }

        public int getMaxTargetsPerRolloutGroup() {
            return maxTargetsPerRolloutGroup;
        }

        public void setMaxTargetsPerRolloutGroup(final int maxTargetsPerRolloutGroup) {
            this.maxTargetsPerRolloutGroup = maxTargetsPerRolloutGroup;
        }

        public int getMaxActionsPerTarget() {
            return maxActionsPerTarget;
        }

        public void setMaxActionsPerTarget(final int maxActionsPerTarget) {
            this.maxActionsPerTarget = maxActionsPerTarget;
        }

        public int getMaxTargetsPerManualAssignment() {
            return maxTargetsPerManualAssignment;
        }

        public void setMaxTargetsPerManualAssignment(final int maxTargetsPerManualAssignment) {
            this.maxTargetsPerManualAssignment = maxTargetsPerManualAssignment;
        }

        public int getMaxTargetsPerAutoAssignment() {
            return maxTargetsPerAutoAssignment;
        }

        public void setMaxTargetsPerAutoAssignment(final int maxTargetsPerAutoAssignment) {
            this.maxTargetsPerAutoAssignment = maxTargetsPerAutoAssignment;
        }

        public void setMaxArtifactSize(final long maxArtifactSize) {
            this.maxArtifactSize = maxArtifactSize;
        }

        public long getMaxArtifactSize() {
            return maxArtifactSize;
        }

        /**
         * Configuration for hawkBits DOS prevention filter. This is usually an
         * infrastructure topic (e.g. Web Application Firewall (WAF)) but might
         * be useful in some cases, e.g. to prevent unintended misuse.
         *
         */
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

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(final boolean enabled) {
                this.enabled = enabled;
            }

            public String getWhitelist() {
                return whitelist;
            }

            public void setWhitelist(final String whitelist) {
                this.whitelist = whitelist;
            }

            public int getMaxRead() {
                return maxRead;
            }

            public void setMaxRead(final int maxRead) {
                this.maxRead = maxRead;
            }

            public int getMaxWrite() {
                return maxWrite;
            }

            public void setMaxWrite(final int maxWrite) {
                this.maxWrite = maxWrite;
            }

        }

    }
}
