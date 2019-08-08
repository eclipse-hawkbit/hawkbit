/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;

/**
 * An pre-authenticated processing filter which extracts (if enabled through
 * configuration) the possibility to authenticate a target based on its target
 * security-token with the {@code Authorization} HTTP header.
 * {@code Example Header: Authorization: HonoToken
 * 5d8fSD54fdsFG98DDsa.}
 *
 * The {@code Authorization} header is a HTTP standard and reverse proxy or
 * other proxies will keep the Authorization headers untouched instead of maybe
 * custom headers which have then weird side-effects. Furthermore frameworks are
 * aware of the sensitivity of the Authorization header and do not log it and
 * store it somewhere.
 */
public class HttpControllerPreAuthenticatedHonoFilter extends AbstractHttpControllerAuthenticationFilter {

    private final ControllerManagement controllerManagement;
    private final String honoCredentialsEndpoint;

    /**
     * Constructor.
     *
     * @param tenantConfigurationManagement
     *            the system management service to retrieve configuration
     *            properties
     * @param tenantAware
     *            the tenant aware service to get configuration for the specific
     *            tenant
     * @param controllerManagement
     *            the controller management to retrieve the specific target
     *            security token to verify
     * @param systemSecurityContext
     *            the system security context
     */
    public HttpControllerPreAuthenticatedHonoFilter(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final ControllerManagement controllerManagement, final SystemSecurityContext systemSecurityContext,
            final String honoCredentialsEndpoint) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
        this.controllerManagement = controllerManagement;
        this.honoCredentialsEndpoint = honoCredentialsEndpoint;
    }

    @Override
    protected PreAuthenticationFilter createControllerAuthenticationFilter() {
        return new ControllerPreAuthenticatedHonoFilter(tenantConfigurationManagement, controllerManagement,
                tenantAware, systemSecurityContext, honoCredentialsEndpoint);
    }

}
