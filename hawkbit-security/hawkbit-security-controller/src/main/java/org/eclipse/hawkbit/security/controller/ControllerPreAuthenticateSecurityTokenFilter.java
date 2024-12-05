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

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;

/**
 * An pre-authenticated processing filter which extracts (if enabled through
 * configuration) the possibility to authenticate a target based on its target
 * security-token with the {@code Authorization} HTTP header.
 * {@code Example Header: Authorization: TargetToken
 * 5d8fSD54fdsFG98DDsa.}
 */
@Slf4j
public class ControllerPreAuthenticateSecurityTokenFilter extends AbstractControllerAuthenticationFilter {

    private static final String TARGET_SECURITY_TOKEN_AUTH_SCHEME = "TargetToken ";
    private static final int OFFSET_TARGET_TOKEN = TARGET_SECURITY_TOKEN_AUTH_SCHEME.length();

    private final ControllerManagement controllerManagement;

    /**
     * Constructor.
     *
     * @param tenantConfigurationManagement the tenant management service to retrieve configuration
     *         properties
     * @param controllerManagement the controller management to retrieve the specific target
     *         security token to verify
     * @param tenantAware the tenant aware service to get configuration for the specific
     *         tenant
     * @param systemSecurityContext the system security context to get access to tenant
     *         configuration
     */
    public ControllerPreAuthenticateSecurityTokenFilter(
            final TenantConfigurationManagement tenantConfigurationManagement,
            final ControllerManagement controllerManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
        this.controllerManagement = controllerManagement;
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedPrincipal(final ControllerSecurityToken securityToken) {
        final String controllerId = resolveControllerId(securityToken);
        final String authHeader = securityToken.getHeader(ControllerSecurityToken.AUTHORIZATION_HEADER);
        if ((authHeader != null) && authHeader.startsWith(TARGET_SECURITY_TOKEN_AUTH_SCHEME)) {
            log.debug("found authorization header with scheme {} using target security token for authentication",
                    TARGET_SECURITY_TOKEN_AUTH_SCHEME);
            return new HeaderAuthentication(controllerId, authHeader.substring(OFFSET_TARGET_TOKEN));
        }
        log.debug(
                "security token filter is enabled but requst does not contain either the necessary path variables {} or the authorization header with scheme {}",
                securityToken, TARGET_SECURITY_TOKEN_AUTH_SCHEME);
        return null;
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedCredentials(final ControllerSecurityToken securityToken) {
        final Optional<Target> target = systemSecurityContext.runAsSystemAsTenant(() -> {
            if (securityToken.getTargetId() != null) {
                return controllerManagement.get(securityToken.getTargetId());
            }
            return controllerManagement.getByControllerId(securityToken.getControllerId());
        }, securityToken.getTenant());

        return target.map(t -> new HeaderAuthentication(t.getControllerId(),
                        systemSecurityContext.runAsSystemAsTenant(() -> t.getSecurityToken(), securityToken.getTenant())))
                .orElse(null);
    }

    @Override
    protected String getTenantConfigurationKey() {
        return TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED;
    }

    private String resolveControllerId(final ControllerSecurityToken securityToken) {
        if (securityToken.getControllerId() != null) {
            return securityToken.getControllerId();
        }
        final Optional<Target> foundTarget = systemSecurityContext.runAsSystemAsTenant(
                () -> controllerManagement.get(securityToken.getTargetId()), securityToken.getTenant());
        return foundTarget.map(Target::getControllerId).orElse(null);
    }
}
