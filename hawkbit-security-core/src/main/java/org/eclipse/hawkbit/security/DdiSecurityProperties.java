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
 * The common properties for DDI security.
 */
@ConfigurationProperties("hawkbit.server.ddi.security")
public class DdiSecurityProperties {

    /**
     * Inner class for reverse proxy configuration. Defines the security
     * properties for authenticating controllers behind a reverse proxy which
     * terminates the SSL session at the reverse proxy but adding request header
     * which contains the CN of the certificate.
     */
    @Component
    @ConfigurationProperties("hawkbit.server.ddi.security.rp")
    public static class RpProperties {

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
     * Inner class for anonymous enable configuration.
     */
    @Component
    @ConfigurationProperties("hawkbit.server.ddi.security.authentication.anonymous")
    public static class AnoymousAuthenticationProperties {

        /**
         * Set to true to enable anonymous DDI client authentication.
         */
        private Boolean enabled = Boolean.FALSE;

        /**
         * @param enabled
         *            the enabled to set
         */
        public void setEnabled(final Boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * @return the enabled
         */
        public Boolean getEnabled() {
            return enabled;
        }

    }

    @Autowired
    private RpProperties rppProperties;

    @Autowired
    private AnoymousAuthenticationProperties authenticationsProperties;

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
        return authenticationsProperties.getEnabled();
    }

}
