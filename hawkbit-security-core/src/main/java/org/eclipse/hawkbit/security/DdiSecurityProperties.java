/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The common properties for DDI security.
 */
@ConfigurationProperties("hawkbit.server.ddi.security")
public class DdiSecurityProperties {

    private final Rp rp = new Rp();
    private final Authentication authentication = new Authentication();

    public Authentication getAuthentication() {
        return authentication;
    }

    public Rp getRp() {
        return rp;
    }

    /**
     * Reverse proxy configuration. Defines the security properties for
     * authenticating controllers behind a reverse proxy which terminates the
     * SSL session at the reverse proxy but adding request header which contains
     * the CN of the certificate.
     */
    public static class Rp {

        /**
         * HTTP header field for common name of a DDI target client certificate.
         */
        private String cnHeader = "X-Ssl-Client-Cn";

        /**
         * HTTP header field for issuer hash of a DDI target client certificate.
         */
        private String sslIssuerHashHeader = "X-Ssl-Issuer-Hash-%d";

        /**
         * List of trusted (reverse proxy) IP addresses for performing DDI
         * client certificate authentication.
         */
        private List<String> trustedIPs;

        /**
         * @return the cnHeader
         */
        public String getCnHeader() {
            return cnHeader;
        }

        /**
         * @param cnHeader
         *            the cnHeader to set
         */
        public void setCnHeader(final String cnHeader) {
            this.cnHeader = cnHeader;
        }

        /**
         * @return the sslIssuerHashHeader
         */
        public String getSslIssuerHashHeader() {
            return sslIssuerHashHeader;
        }

        /**
         * @param sslIssuerHashHeader
         *            the sslIssuerHashHeader to set
         */
        public void setSslIssuerHashHeader(final String sslIssuerHashHeader) {
            this.sslIssuerHashHeader = sslIssuerHashHeader;
        }

        /**
         * @return the trustedIPs
         */
        public List<String> getTrustedIPs() {
            return trustedIPs;
        }

        /**
         * @param trustedIPs
         *            the trustedIPs to set
         */
        public void setTrustedIPs(final List<String> trustedIPs) {
            this.trustedIPs = trustedIPs;
        }

    }

    /**
     * DDI Authentication options.
     */
    public static class Authentication {
        private final Anonymous anonymous = new Anonymous();
        private final Targettoken targettoken = new Targettoken();
        private final Gatewaytoken gatewaytoken = new Gatewaytoken();

        public Anonymous getAnonymous() {
            return anonymous;
        }

        public Gatewaytoken getGatewaytoken() {
            return gatewaytoken;
        }

        public Targettoken getTargettoken() {
            return targettoken;
        }

        /**
         * Target token authentication. Tokens are defined per target.
         *
         */
        public static class Targettoken {
            /**
             * Set to true to enable target token authentication.
             */
            private boolean enabled = false;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(final boolean enabled) {
                this.enabled = enabled;
            }

        }

        /**
         * Gateway token authentication. Tokens are defined per tenant. Use with
         * care!
         *
         */
        public static class Gatewaytoken {

            /**
             * Gateway token based authentication enabled.
             */
            private boolean enabled = false;

            /**
             * Default gateway token name.
             */
            private String name = "";

            /**
             * Default gateway token itself.
             */
            private String key = "";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(final boolean enabled) {
                this.enabled = enabled;
            }

            public String getName() {
                return name;
            }

            public void setName(final String name) {
                this.name = name;
            }

            public String getKey() {
                return key;
            }

            public void setKey(final String key) {
                this.key = key;
            }

        }

        /**
         * Anonymous authentication.
         */
        public static class Anonymous {

            /**
             * Set to true to enable anonymous DDI client authentication.
             */
            private boolean enabled = false;

            /**
             * @param enabled
             *            the enabled to set
             */
            public void setEnabled(final boolean enabled) {
                this.enabled = enabled;
            }

            /**
             * @return the enabled
             */
            public boolean isEnabled() {
                return enabled;
            }
        }

    }

}
