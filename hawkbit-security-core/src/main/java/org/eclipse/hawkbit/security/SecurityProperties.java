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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The common properties for security.
 * 
 *
 *
 */
@ConfigurationProperties
public class SecurityProperties {

    @Component
    @ConfigurationProperties("hawkbit.server.controller.security.rp")
    public static class RpProperties {
        private String cnHeader = "X-Ssl-Client-Cn";
        private String sslIssuerHashHeader = "X-Ssl-Issuer-Hash-%d";
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

    @Component
    @ConfigurationProperties("hawkbit.server.controller.security.authentication")
    public static class AuthenticationsProperties {
        private Boolean anonymousEnabled = Boolean.FALSE;

        /**
         * @param anonymousEnabled
         *            the anonymousEnabled to set
         */
        public void setAnonymousEnabled(final Boolean anonymousEnabled) {
            this.anonymousEnabled = anonymousEnabled;
        }

        /**
         * @return the anonymousEnabled
         */
        public Boolean getAnonymousEnabled() {
            return anonymousEnabled;
        }

    }

    @Autowired
    private RpProperties rppProperties;

    @Autowired
    private AuthenticationsProperties authenticationsProperties;

    public String getRpCnHeader() {
        return rppProperties.getCnHeader();
    }

    public String getRpSslIssuerHashHeader() {
        return rppProperties.getSslIssuerHashHeader();
    }

    public List<String> getRpTrustedIPs() {
        return rppProperties.getTrustedIPs();
    }

    public Boolean getAnonymousEnabled() {
        return authenticationsProperties.getAnonymousEnabled();
    }

}
