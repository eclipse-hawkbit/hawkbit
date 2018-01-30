/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    public HeaderAuthentication getPreAuthenticatedPrincipal(final DmfTenantSecurityToken secruityToken) {
        return new HeaderAuthentication(secruityToken.getControllerId(), secruityToken.getControllerId());
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedCredentials(final DmfTenantSecurityToken secruityToken) {
        return new HeaderAuthentication(secruityToken.getControllerId(), secruityToken.getControllerId());
    }

    @Override
    public boolean isEnable(final DmfTenantSecurityToken secruityToken) {
        return ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled();
    }

}
