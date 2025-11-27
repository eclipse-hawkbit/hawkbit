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

import static org.eclipse.hawkbit.context.AccessContext.asTenant;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_HEADER_AUTHORITY_NAME;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

/**
 * An authenticator which extracts the principal from a request URI and the credential from a request header in a the
 * {@link ControllerSecurityToken}.
 */
@Slf4j
public class SecurityHeaderAuthenticator extends Authenticator.AbstractAuthenticator {

    private static final Logger LOG_SECURITY_AUTH = LoggerFactory.getLogger("server-security.auth");

    // Example Headers with Cert Information
    // Clientip: 217.24.201.180
    // X-Forwarded-Proto: https
    // X-Ssl-Client-Cn: my.name
    // X-Ssl-Client-Dn: CN=my.name,CN=O,CN=R,CN=DE,CN=BOSCH,CN=pki,DC=bosch,DC=com
    // X-Ssl-Client-Hash: 7f:87:cb:b5:9c:e0:c5:0a:1a:a6:57:69:0f:ca:0a:95
    // X-Ssl-Client-Notafter: Dec 18 08:02:45 2017 GMT
    // X-Ssl-Client-Notbefore: Dec 18 07:32:45 2014 GMT
    // X-Ssl-Client-Verify: ok
    // X-Ssl-Issuer: CN=Bosch-CA1-DE,CN=PKI,DC=Bosch,DC=com
    // X-Ssl-Issuer-Dn-1: CN=Bosch-CA-DE,CN=PKI,DC=Bosch,DC=com
    // X-Ssl-Issuer-Hash-1: ae:11:f5:6a:0a:e8:74:50:81:0e:0c:37:ec:c5:22:fc
    private final String caCommonNameHeader;
    // the X-Ssl-Issuer-Hash basic header: Contains the x509 fingerprint hash, this
    // header exists multiple times in the request for all trusted chains.
    private final String sslIssuerHashBasicHeader;

    public SecurityHeaderAuthenticator(final String caCommonNameHeader, final String caAuthorityNameHeader) {
        this.caCommonNameHeader = caCommonNameHeader;
        this.sslIssuerHashBasicHeader = caAuthorityNameHeader;
    }

    @Override
    public Authentication authenticate(final ControllerSecurityToken controllerSecurityToken) {
        // retrieve the common name header and the authority name header from the http request and combine them together
        final String commonNameValue = controllerSecurityToken.getHeader(caCommonNameHeader);
        if (commonNameValue == null) {
            log.debug("The request doesn't contain the 'common name' header");
            return null;
        }
        if (!commonNameValue.equals(controllerSecurityToken.getControllerId())) {
            log.debug("The request contains the 'common name' header but it doesn't match the controller id");
            return null;
        }

        if (!isEnabled(controllerSecurityToken)) {
            log.debug("The gateway header auth is disabled");
            return null;
        }

        final String sslIssuerHashValue = getIssuerHashHeader(
                controllerSecurityToken,
                asTenant(
                        controllerSecurityToken.getTenant(),
                        () -> TenantConfigHelper.getAsSystem(AUTHENTICATION_HEADER_AUTHORITY_NAME, String.class)));
        if (sslIssuerHashValue == null) {
            log.debug("The request contains the 'common name' header but trusted hash is not found");
            return null;
        }
        if (log.isTraceEnabled()) {
            log.debug("Found sslIssuerHash ****, using as credentials for tenant {}", controllerSecurityToken.getTenant());
        }

        return authenticatedController(controllerSecurityToken.getTenant(), commonNameValue);
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
     * Iterates over the {@link #sslIssuerHashBasicHeader} basic header {@code X-Ssl-Issuer-Hash-%d} and try to find the same hash as known.
     * It's ok if we find the hash in any the trusted CA chain to accept this request for this tenant.
     */
    @SuppressWarnings("java:S2629") // check if debug is enabled is maybe heavier then evaluation
    private String getIssuerHashHeader(final ControllerSecurityToken controllerSecurityToken, final String knownIssuerHashes) {
        // there may be several knownIssuerHashes configured for the tenant
        final List<String> knownHashes = Arrays.stream(knownIssuerHashes.split("[;,]")).map(String::toLowerCase).toList();

        // iterate over the headers until we get a null header.
        String foundHash;
        for (int iHeader = 1; (foundHash = controllerSecurityToken.getHeader(
                String.format(sslIssuerHashBasicHeader, iHeader))) != null; iHeader++) {
            if (knownHashes.contains(foundHash.toLowerCase())) {
                if (log.isTraceEnabled()) {
                    log.trace("Found matching ssl issuer hash at position {}", iHeader);
                }
                return foundHash.toLowerCase();
            }
        }
        LOG_SECURITY_AUTH.debug(
                "Certificate request but no matching hash found in headers {} for common name {} in request",
                sslIssuerHashBasicHeader, controllerSecurityToken.getHeader(caCommonNameHeader));
        return null;
    }
}