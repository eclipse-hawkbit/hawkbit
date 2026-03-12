/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security.controller;

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

    /**
     * Reverse proxy configuration. Defines the security properties for
     * authenticating controllers behind a reverse proxy which terminates the
     * SSL session at the reverse proxy but adding request header which contains
     * the CN of the certificate.
     */
    @Data
    public static class Rp {

        /**
         * HTTP header field for controller ID (e.g. CN of the controller certificate) of a DDI target client certificate.
         */
        private String controllerIdHeader = "X-Controller-Id";
        /**
         * HTTP header field for authority(ies) (e.g. SHA-256 fingerprints of issuer certificates) of a DDI target client certificate.
         */
        private String authorityHeader = "X-Authority";
        /**
         * Regular expression for authorities list separator
         */
        private String authoritiesSeparatorRegex = "[;,]";
        /**
         * List of trusted (reverse proxy) IP addresses for performing DDI client certificate auth.
         */
        private List<String> trustedIPs;
    }

    /**
     * DDI Authentication options.
     */
    @Data
    public static class Authentication {

        private final TargetToken targettoken = new TargetToken();
        private final GatewayToken gatewaytoken = new GatewayToken();

        /**
         * Target token auth. Tokens are defined per target.
         */
        @Data
        public static class TargetToken {

            /**
             * Set to true to enable target token auth.
             */
            private boolean enabled = false;
        }

        /**
         * Gateway token auth. Tokens are defined per tenant. Use with care!
         */
        @Data
        public static class GatewayToken {

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
            @ToString.Exclude
            private String key = "";
        }
    }
}