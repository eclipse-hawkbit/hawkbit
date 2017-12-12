/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An pre-authenticated processing filter which extracts (if enabled through
 * configuration) the possibility to authenticate a target based through a
 * gateway security token. This is commonly used for targets connected
 * indirectly via a gateway. This gateway controls multiple targets under the
 * gateway security token which can be set via the {@code TenantSecruityToken}
 * header. {@code Example Header: Authorization: GatewayToken
 * 5d8fSD54fdsFG98DDsa.}
 * 
 *
 *
 */
public class ControllerPreAuthenticatedGatewaySecurityTokenFilter extends AbstractControllerAuthenticationFilter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ControllerPreAuthenticatedGatewaySecurityTokenFilter.class);
    private static final String GATEWAY_SECURITY_TOKEN_AUTH_SCHEME = "GatewayToken ";
    private static final int OFFSET_GATEWAY_TOKEN = GATEWAY_SECURITY_TOKEN_AUTH_SCHEME.length();

    private final GetGatewaySecurityConfigurationKeyTenantRunner gatewaySecurityTokenKeyConfigRunner = new GetGatewaySecurityConfigurationKeyTenantRunner();

    /**
     * Constructor.
     * 
     * @param tenantConfigurationManagement
     *            the tenant management service to retrieve configuration
     *            properties
     * @param tenantAware
     *            the tenant aware service to get configuration for the specific
     *            tenant
     * @param systemSecurityContext
     *            the system security context to get access to tenant
     *            configuration
     */
    public ControllerPreAuthenticatedGatewaySecurityTokenFilter(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedPrincipal(final DmfTenantSecurityToken secruityToken) {
        final String authHeader = secruityToken.getHeader(DmfTenantSecurityToken.AUTHORIZATION_HEADER);
        if ((authHeader != null) && authHeader.startsWith(GATEWAY_SECURITY_TOKEN_AUTH_SCHEME)) {
            LOGGER.debug("found authorization header with scheme {} using target security token for authentication",
                    GATEWAY_SECURITY_TOKEN_AUTH_SCHEME);
            return new HeaderAuthentication(secruityToken.getControllerId(),
                    authHeader.substring(OFFSET_GATEWAY_TOKEN));
        }
        LOGGER.debug(
                "security token filter is enabled but request does not contain either the necessary secruity token {} or the authorization header with scheme {}",
                secruityToken, GATEWAY_SECURITY_TOKEN_AUTH_SCHEME);
        return null;
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedCredentials(final DmfTenantSecurityToken secruityToken) {
        final String gatewayToken = tenantAware.runAsTenant(secruityToken.getTenant(),
                gatewaySecurityTokenKeyConfigRunner);
        return new HeaderAuthentication(secruityToken.getControllerId(), gatewayToken);
    }

    @Override
    protected String getTenantConfigurationKey() {
        return TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED;
    }

    private final class GetGatewaySecurityConfigurationKeyTenantRunner implements TenantAware.TenantRunner<String> {

        @Override
        public String run() {
            LOGGER.trace("retrieving configuration value for configuration key {}",
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY);

            return systemSecurityContext
                    .runAsSystem(() -> tenantConfigurationManagement
                            .getConfigurationValue(
                                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class)
                            .getValue());
        }
    }

}
