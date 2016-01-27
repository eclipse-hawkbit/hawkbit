/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.eclipse.hawkbit.dmf.json.model.TenantSecruityToken;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * An pre-authenticated processing filter which extracts (if enabled through
 * configuration) the possibility to authenticate a target based on its target
 * security-token with the {@code Authorization} HTTP header.
 * {@code Example Header: Authorization: TargetToken
 * 5d8fSD54fdsFG98DDsa.}
 * 
 *
 *
 */
public class ControllerPreAuthenticateSecurityTokenFilter extends AbstractControllerAuthenticationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerPreAuthenticateSecurityTokenFilter.class);
    private static final String TARGET_SECURITY_TOKEN_AUTH_SCHEME = "TargetToken ";
    private static final int OFFSET_TARGET_TOKEN = TARGET_SECURITY_TOKEN_AUTH_SCHEME.length();

    private final ControllerManagement controllerManagement;

    /**
     * Constructor.
     * 
     * @param systemManagement
     *            the system management service to retrieve configuration
     *            properties
     * @param controllerManagement
     *            the controller management to retrieve the specific target
     *            security token to verify
     * @param tenantAware
     *            the tenant aware service to get configuration for the specific
     *            tenant
     */
    public ControllerPreAuthenticateSecurityTokenFilter(
            final TenantConfigurationManagement tenantConfigurationManagement,
            final ControllerManagement controllerManagement, final TenantAware tenantAware) {
        super(tenantConfigurationManagement, tenantAware);
        this.controllerManagement = controllerManagement;
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedPrincipal(final TenantSecruityToken secruityToken) {
        final String authHeader = secruityToken.getHeader(TenantSecruityToken.AUTHORIZATION_HEADER);
        if ((authHeader != null) && authHeader.startsWith(TARGET_SECURITY_TOKEN_AUTH_SCHEME)) {
            LOGGER.debug("found authorization header with scheme {} using target security token for authentication",
                    TARGET_SECURITY_TOKEN_AUTH_SCHEME);
            return new HeaderAuthentication(secruityToken.getControllerId(), authHeader.substring(OFFSET_TARGET_TOKEN));
        }
        LOGGER.debug(
                "security token filter is enabled but requst does not contain either the necessary path variables {} or the authorization header with scheme {}",
                secruityToken, TARGET_SECURITY_TOKEN_AUTH_SCHEME);
        return null;
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedCredentials(final TenantSecruityToken secruityToken) {
        final String securityToken = tenantAware.runAsTenant(secruityToken.getTenant(),
                new GetSecurityTokenTenantRunner(secruityToken.getTenant(), secruityToken.getControllerId()));
        return new HeaderAuthentication(secruityToken.getControllerId(), securityToken);
    }

    @Override
    protected TenantConfigurationKey getTenantConfigurationKey() {
        return TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED;
    }

    private final class GetSecurityTokenTenantRunner implements TenantAware.TenantRunner<String> {

        private final String controllerId;
        private final String tenant;

        private GetSecurityTokenTenantRunner(final String tenant, final String controllerId) {
            this.tenant = tenant;
            this.controllerId = controllerId;
        }

        @Override
        public String run() {
            LOGGER.trace("retrieving security token for controllerId {}", controllerId);
            final SecurityContext oldContext = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(getSecurityTokenReadContext());
                return controllerManagement.getSecurityTokenByControllerId(controllerId);
            } finally {
                SecurityContextHolder.setContext(oldContext);
            }
        }

        private SecurityContext getSecurityTokenReadContext() {
            final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
            securityContextImpl.setAuthentication(getSecurityTokenReadAuthentication());
            return securityContextImpl;
        }

        private Authentication getSecurityTokenReadAuthentication() {
            final AnonymousAuthenticationToken anonymousAuthenticationToken = new AnonymousAuthenticationToken(
                    "anonymous-read-security-token", "anonymous", com.google.common.collect.Lists
                            .newArrayList(new SimpleGrantedAuthority(SpPermission.READ_TARGET_SEC_TOKEN)));
            anonymousAuthenticationToken.setDetails(new TenantAwareAuthenticationDetails(tenant, true));
            return anonymousAuthenticationToken;
        }
    }
}
