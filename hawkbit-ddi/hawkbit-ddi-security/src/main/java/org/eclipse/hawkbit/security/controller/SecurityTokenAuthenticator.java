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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

/**
 * An authenticator which extracts (if enabled through configuration) the possibility to authenticate a target based on
 * its target security-token with the {@code Authorization} HTTP header.
 * <p>
 * {@code Example Header: Authorization: TargetToken 5d8fSD54fdsFG98DDsa.}
 */
@Slf4j
public class SecurityTokenAuthenticator extends Authenticator.AbstractAuthenticator {

    public static final String TARGET_SECURITY_TOKEN_AUTH_SCHEME = "TargetToken ";
    private static final int OFFSET_TARGET_TOKEN = TARGET_SECURITY_TOKEN_AUTH_SCHEME.length();

    private final ControllerManagement controllerManagement;

    public SecurityTokenAuthenticator(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext,
            final ControllerManagement controllerManagement) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
        this.controllerManagement = controllerManagement;
    }

    @Override
    public Authentication authenticate(final ControllerSecurityToken controllerSecurityToken) {
        final String authHeader = controllerSecurityToken.getHeader(ControllerSecurityToken.AUTHORIZATION_HEADER);
        if (authHeader == null) {
            log.debug("The request doesn't contain the 'authorization' header");
            return null;
        } else if (!authHeader.startsWith(TARGET_SECURITY_TOKEN_AUTH_SCHEME)) {
            log.debug("The request contains the 'authorization' header but it doesn't start with '{}'", TARGET_SECURITY_TOKEN_AUTH_SCHEME);
            return null;
        }

        if (!isEnabled(controllerSecurityToken)) {
            log.debug("The target security token authentication is disabled");
            return null;
        }

        log.debug("Found 'authorization' header starting with '{}'", TARGET_SECURITY_TOKEN_AUTH_SCHEME);
        final String presentedToken = authHeader.substring(OFFSET_TARGET_TOKEN);

        return systemSecurityContext.runAsSystemAsTenant(() -> controllerSecurityToken.getTargetId() != null
                                ? controllerManagement.get(controllerSecurityToken.getTargetId())
                                : controllerManagement.getByControllerId(controllerSecurityToken.getControllerId()),
                        controllerSecurityToken.getTenant())
                // validate if the presented token is the same as the one set for the target
                .filter(target -> presentedToken.equals(
                        systemSecurityContext.runAsSystemAsTenant(target::getSecurityToken, controllerSecurityToken.getTenant())))
                .map(target -> authenticatedController(controllerSecurityToken.getTenant(), target.getControllerId()))
                .orElse(null);
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    protected String getTenantConfigurationKey() {
        return TenantConfigurationKey.AUTHENTICATION_TARGET_SECURITY_TOKEN_ENABLED;
    }
}