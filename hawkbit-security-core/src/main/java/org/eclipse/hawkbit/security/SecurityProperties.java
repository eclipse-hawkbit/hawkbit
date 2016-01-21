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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The common properties for security.
 * 
 *
 *
 */
@ConfigurationProperties
public class SecurityProperties {

    @Value("${hawkbit.server.controller.security.rp.cnHeader:X-Ssl-Client-Cn}")
    private String rpCnHeader;

    @Value("${hawkbit.server.controller.security.rp.sslIssuerHashHeader:X-Ssl-Issuer-Hash-%d}")
    private String rpSslIssuerHashHeader;

    @Value("${hawkbit.server.controller.security.rp.trustedIPs:#{null}}")
    private List<String> rpTrustedIPs;

    @Value("${hawkbit.server.controller.security.authentication.anonymous.enabled:false}")
    private Boolean anonymousEnabled;

    public String getRpCnHeader() {
        return rpCnHeader;
    }

    public String getRpSslIssuerHashHeader() {
        return rpSslIssuerHashHeader;
    }

    public List<String> getRpTrustedIPs() {
        return rpTrustedIPs;
    }

    public Boolean getAnonymousEnabled() {
        return anonymousEnabled;
    }

    public void setRpCnHeader(final String rpCnHeader) {
        this.rpCnHeader = rpCnHeader;
    }

    public void setRpSslIssuerHashHeader(final String rpSslIssuerHashHeader) {
        this.rpSslIssuerHashHeader = rpSslIssuerHashHeader;
    }

    public void setRpTrustedIPs(final List<String> rpTrustedIPs) {
        this.rpTrustedIPs = rpTrustedIPs;
    }

    public void setAnonymousEnabled(final Boolean anonymousEnabled) {
        this.anonymousEnabled = anonymousEnabled;
    }
}
