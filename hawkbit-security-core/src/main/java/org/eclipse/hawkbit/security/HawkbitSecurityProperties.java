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
import org.springframework.stereotype.Component;

/**
 * Security related hawkbit configuration.
 *
 */
@Component
@ConfigurationProperties("hawkbit.server.security")
public class HawkbitSecurityProperties {

    private final Clients clients = new Clients();
    private final Dos dos = new Dos();
    private final Xframe xframe = new Xframe();

    public Dos getDos() {
        return dos;
    }

    public Clients getClients() {
        return clients;
    }

    public Xframe getXframe() {
        return xframe;
    }

    /**
     * Defines the XFrameOption policy.
     *
     */
    public static class Xframe {

        /**
         * XFrame option. Allowed values: SAMEORIGIN, DENY, ALLOW-FROM
         */
        private String option = "DENY";

        /**
         * ALLOW-FROM defined URL, has to be filled in case ALLOW-FROM option is
         * selected.
         */
        private String allowfrom = "";

        public String getOption() {
            return option;
        }

        public void setOption(final String option) {
            this.option = option;
        }

        public String getAllowfrom() {
            return allowfrom;
        }

        public void setAllowfrom(final String allowfrom) {
            this.allowfrom = allowfrom;
        }

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
         * Name of the http header from which the remote ip is extracted for DDI
         * connected clients.
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

        private final Filter filter = new Filter();

        public Filter getFilter() {
            return filter;
        }

        public int getMaxStatusEntriesPerAction() {
            return maxStatusEntriesPerAction;
        }

        public void setMaxStatusEntriesPerAction(final int maxStatusEntriesPerAction) {
            this.maxStatusEntriesPerAction = maxStatusEntriesPerAction;
        }

        public int getMaxAttributeEntriesPerTarget() {
            return maxAttributeEntriesPerTarget;
        }

        public void setMaxAttributeEntriesPerTarget(final int maxAttributeEntriesPerTarget) {
            this.maxAttributeEntriesPerTarget = maxAttributeEntriesPerTarget;
        }

        /**
         * Configuration for hawkBits DOS prevention filter. This is usually an
         * infrastructure topic but might be useful in some cases.
         *
         */
        public static class Filter {

            /**
             * White list of peer IP addresses for DOS filter (regular
             * expression).
             */
            private String whitelist = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|169\\.254\\.\\d{1,3}\\.\\d{1,3}|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}";

            /**
             * # Maximum number of allowed REST read/GET requests per second per
             * client.
             */
            int maxRead = 200;

            /**
             * Maximum number of allowed REST write/(PUT/POST/etc.) requests per
             * second per client.
             */
            int maxWrite = 50;

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
