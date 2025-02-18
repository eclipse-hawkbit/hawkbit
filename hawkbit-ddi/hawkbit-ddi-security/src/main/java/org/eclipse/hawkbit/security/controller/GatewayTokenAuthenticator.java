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
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

/**
 * An authenticator which extracts (if enabled through configuration) the possibility to authenticate a target based through
 * a gateway security token. This is commonly used for targets connected indirectly via a gateway. This gateway controls multiple targets
 * under the gateway security token which can be set via the {@code Authorization} header.
 * <p>
 * {@code Example Header: Authorization: GatewayToken 5d8fSD54fdsFG98DDsa.}
 */
@Slf4j
public class GatewayTokenAuthenticator extends Authenticator.AbstractAuthenticator {

    public static final String GATEWAY_SECURITY_TOKEN_AUTH_SCHEME = "GatewayToken ";
    private static final int OFFSET_GATEWAY_TOKEN = GATEWAY_SECURITY_TOKEN_AUTH_SCHEME.length();

    private final TenantAware.TenantRunner<String> gatewaySecurityTokenKeyConfigRunner;

    public GatewayTokenAuthenticator(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
        gatewaySecurityTokenKeyConfigRunner = () -> {
            log.trace("retrieving configuration value for configuration key {}",
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY);

            return systemSecurityContext
                    .runAsSystem(() -> tenantConfigurationManagement
                            .getConfigurationValue(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class)
                            .getValue());
        };
    }

    @Override
    public Authentication authenticate(final ControllerSecurityToken controllerSecurityToken) {
        final String authHeader = controllerSecurityToken.getHeader(ControllerSecurityToken.AUTHORIZATION_HEADER);
        if (authHeader == null) {
            log.debug("The request doesn't contain the 'authorization' header");
            return null;
        } else if (!authHeader.startsWith(GATEWAY_SECURITY_TOKEN_AUTH_SCHEME)) {
            log.debug("The request contains the 'authorization' header but it doesn't start with '{}'", GATEWAY_SECURITY_TOKEN_AUTH_SCHEME);
            return null;
        }

        if (!isEnabled(controllerSecurityToken)) {
            log.debug("The gateway token authentication is disabled");
            return null;
        }

        log.debug("Found 'authorization' header starting with '{}'", GATEWAY_SECURITY_TOKEN_AUTH_SCHEME);
        final String presentedToken = authHeader.substring(OFFSET_GATEWAY_TOKEN);

        // validate if the presented token is the same as the gateway token
        return presentedToken.equals(tenantAware.runAsTenant(controllerSecurityToken.getTenant(), gatewaySecurityTokenKeyConfigRunner))
                ? authenticatedController(controllerSecurityToken.getTenant(), controllerSecurityToken.getControllerId()) : null;
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    protected String getTenantConfigurationKey() {
        return TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED;
    }
}