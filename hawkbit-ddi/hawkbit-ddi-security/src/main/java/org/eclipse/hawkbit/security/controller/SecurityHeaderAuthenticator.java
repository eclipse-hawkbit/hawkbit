/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security.controller;

import static org.eclipse.hawkbit.audit.SecurityLogger.LOGGER;
import static org.eclipse.hawkbit.context.AccessContext.asTenant;
import static org.eclipse.hawkbit.repository.helper.TenantConfigHelper.getAsSystem;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_HEADER_AUTHORITY;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

/**
 * An authenticator which extracts the principal from a request URI and the credential from a request header in a the
 * {@link ControllerSecurityToken}.
 */
@Slf4j
public class SecurityHeaderAuthenticator extends Authenticator.AbstractAuthenticator {

    // e.g: X-Controller-Id: controller-1 or X-Subject-CN: controller-1
    private final String controllerIdHeader;
    // e.g.: X-Authority: X,Y or X-CA-Fingerprint-0: <e.g. SHA-256 fingerprint>
    // could be used with one or multiple authorities that has confirmed the controller id:
    // 1. <authority>=X,Y,Z -> comma separated list of authorities (could be single authority)
    // 2. <authority>-0=X, <authority>-1=Y, .. ... until we get a null header
    private final String authorityHeader;
    private final String authoritiesSeparatorRegex;

    public SecurityHeaderAuthenticator(final DdiSecurityProperties.Rp rp) {
        this.controllerIdHeader = rp.getControllerIdHeader();
        this.authorityHeader = rp.getAuthorityHeader();
        this.authoritiesSeparatorRegex = rp.getAuthoritiesSeparatorRegex();
    }

    @Override
    public Authentication authenticate(final ControllerSecurityToken controllerSecurityToken) {
        // retrieve the common name header and the authority name header from the http request and combine them together
        final String verifiedControllerId = controllerSecurityToken.getHeader(controllerIdHeader);
        if (verifiedControllerId == null) {
            log.debug("The request doesn't contain the '{}' header", controllerIdHeader);
            return null;
        }
        if (!verifiedControllerId.equals(controllerSecurityToken.getControllerId())) {
            log.debug("The request contains the '{}' header but it doesn't match the controller id", controllerIdHeader);
            return null;
        }

        if (!isEnabled(controllerSecurityToken)) { // in order to do not do calls to db - check after previous header checks
            log.debug("The gateway header authentication is disabled");
            return null;
        }

        final String tenant = controllerSecurityToken.getTenant();
        if (verify(controllerSecurityToken, asTenant(tenant, () -> getAsSystem(AUTHENTICATION_HEADER_AUTHORITY, String.class)))) {
            log.trace("Found trusted authority ****, using as credentials (tenant: {})", tenant);
            return authenticatedController(tenant, verifiedControllerId);
        } else {
            return null;
        }
    }

    @Override
    protected String getTenantConfigurationKey() {
        return TenantConfigurationKey.AUTHENTICATION_HEADER_ENABLED;
    }

    @Override
    public Logger log() {
        return log;
    }

    /**
     * Check {@link #authorityHeader} basic header or iterates over {@link #authorityHeader}-%d and try to find the same authority as trusted.
     * It's ok if we find any authority (in headers, authenticated the controller) to accept this request for this tenant.
     */
    @SuppressWarnings({ "java:S2629", "java:S135", "java:S3776" }) // check if debug is enabled is maybe heavier than evaluation, rest - fine
    private boolean verify(final ControllerSecurityToken controllerSecurityToken, final String headerAuthority) {
        // there may be several trusted authorities (headerAuthority config value) configured for the tenant
        final List<String> trustedAuthorities = Arrays.stream(headerAuthority.split(authoritiesSeparatorRegex))
                .map(String::toLowerCase).map(String::trim).toList();

        boolean hasAuthorityHeader = false;
        String matchingAuthority = null;

        final String authorityHeaderValue = controllerSecurityToken.getHeader(authorityHeader);
        if (authorityHeaderValue == null) {
            // go for authority header prefixed iteration. iterate over the headers until we get a null header. Start from index 0 or 1
            for (int i = 0; ; i++) {
                final String authority = controllerSecurityToken.getHeader(authorityHeader + "-" + i);
                if (authority == null) {
                    if (i != 0) {
                        break; // end of index iteration
                    } // if 0, try if start from 1
                } else {
                    hasAuthorityHeader = true;
                    final String authorityLower = authority.toLowerCase();
                    if (trustedAuthorities.contains(authorityLower)) {
                        matchingAuthority = authorityLower;
                        break;
                    }
                }
            }
        } else {
            hasAuthorityHeader = true;
            matchingAuthority = Arrays.stream(authorityHeaderValue.split(authoritiesSeparatorRegex))
                    .map(String::toLowerCase).map(String::trim)
                    .filter(trustedAuthorities::contains)
                    .findFirst().orElse(null);
        }

        if (matchingAuthority == null) {
            if (hasAuthorityHeader) {
                LOGGER.debug("[SEC_HEADER_AUTH] Request has an authority header(s) but it doesn't match any trusted authority");
            }
        } else if (log.isTraceEnabled()) {
            log.trace("Found matching authority {}", matchingAuthority);
        }

        return matchingAuthority != null;
    }
}