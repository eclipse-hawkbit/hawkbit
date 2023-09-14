/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

/**
 * An anonymous controller filter which is only enabled in case of anonymous
 * access is granted. This should only be for development purposes.
 * 
 * @see DdiSecurityProperties
 */
public class ControllerPreAuthenticatedAnonymousFilter implements PreAuthenticationFilter {

    private final DdiSecurityProperties ddiSecurityConfiguration;

    /**
     * @param ddiSecurityConfiguration
     *            the security configuration which holds the configuration if
     *            anonymous is enabled or not
     */
    public ControllerPreAuthenticatedAnonymousFilter(final DdiSecurityProperties ddiSecurityConfiguration) {
        this.ddiSecurityConfiguration = ddiSecurityConfiguration;
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedPrincipal(final DmfTenantSecurityToken securityToken) {
        return new HeaderAuthentication(securityToken.getControllerId(), securityToken.getControllerId());
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedCredentials(final DmfTenantSecurityToken securityToken) {
        return new HeaderAuthentication(securityToken.getControllerId(), securityToken.getControllerId());
    }

    @Override
    public boolean isEnable(final DmfTenantSecurityToken securityToken) {
        return ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled();
    }

}
