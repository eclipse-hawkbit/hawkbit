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

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The common properties for DDI security.
 */
@Getter
@EqualsAndHashCode
@ToString
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
    @Data
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
         * client certificate auth.
         */
        private List<String> trustedIPs;
    }

    /**
     * DDI Authentication options.
     */
    @Data
    public static class Authentication {

        private final Targettoken targettoken = new Targettoken();
        private final Gatewaytoken gatewaytoken = new Gatewaytoken();

        /**
         * Target token auth. Tokens are defined per target.
         */
        @Data
        public static class Targettoken {

            /**
             * Set to true to enable target token auth.
             */
            private boolean enabled = false;
        }

        /**
         * Gateway token auth. Tokens are defined per tenant. Use with care!
         */
        @Data
        public static class Gatewaytoken {

            /**
             * Gateway token based auth enabled.
             */
            private boolean enabled = false;

            /**
             * Default gateway token name.
             */
            private String name = "";

            /**
             * Default gateway token itself.
             */
            @ToString.Exclude
            private String key = "";
        }
    }
}