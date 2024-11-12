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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;

/**
 * An pre-authenticated processing filter which extracts (if enabled through
 * configuration) the possibility to authenticate a target based through a
 * gateway security token. This is commonly used for targets connected
 * indirectly via a gateway. This gateway controls multiple targets under the
 * gateway security token which can be set via the {@code TenantsecurityToken}
 * header. {@code Example Header: Authorization: GatewayToken
 * 5d8fSD54fdsFG98DDsa.}
 */
@Slf4j
public class ControllerPreAuthenticatedGatewaySecurityTokenFilter extends AbstractControllerAuthenticationFilter {

    private static final String GATEWAY_SECURITY_TOKEN_AUTH_SCHEME = "GatewayToken ";
    private static final int OFFSET_GATEWAY_TOKEN = GATEWAY_SECURITY_TOKEN_AUTH_SCHEME.length();

    private final GetGatewaySecurityConfigurationKeyTenantRunner gatewaySecurityTokenKeyConfigRunner = new GetGatewaySecurityConfigurationKeyTenantRunner();

    /**
     * Constructor.
     *
     * @param tenantConfigurationManagement the tenant management service to retrieve configuration
     *         properties
     * @param tenantAware the tenant aware service to get configuration for the specific
     *         tenant
     * @param systemSecurityContext the system security context to get access to tenant
     *         configuration
     */
    public ControllerPreAuthenticatedGatewaySecurityTokenFilter(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedPrincipal(final ControllerSecurityToken securityToken) {
        final String authHeader = securityToken.getHeader(ControllerSecurityToken.AUTHORIZATION_HEADER);
        if (authHeader != null &&
                authHeader.startsWith(GATEWAY_SECURITY_TOKEN_AUTH_SCHEME) &&
                authHeader.length() > OFFSET_GATEWAY_TOKEN) { // disables empty string token
            log.debug("found authorization header with scheme {} using target security token for authentication",
                    GATEWAY_SECURITY_TOKEN_AUTH_SCHEME);
            return new HeaderAuthentication(securityToken.getControllerId(),
                    authHeader.substring(OFFSET_GATEWAY_TOKEN));
        }
        log.debug(
                "security token filter is enabled but request does not contain either the necessary security token {} or the authorization header with scheme {}",
                securityToken, GATEWAY_SECURITY_TOKEN_AUTH_SCHEME);
        return null;
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedCredentials(final ControllerSecurityToken securityToken) {
        final String gatewayToken = tenantAware.runAsTenant(securityToken.getTenant(),
                gatewaySecurityTokenKeyConfigRunner);
        return new HeaderAuthentication(securityToken.getControllerId(), gatewayToken);
    }

    @Override
    protected String getTenantConfigurationKey() {
        return TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED;
    }

    private final class GetGatewaySecurityConfigurationKeyTenantRunner implements TenantAware.TenantRunner<String> {

        @Override
        public String run() {
            log.trace("retrieving configuration value for configuration key {}",
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY);

            return systemSecurityContext
                    .runAsSystem(() -> tenantConfigurationManagement
                            .getConfigurationValue(
                                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class)
                            .getValue());
        }
    }

}
