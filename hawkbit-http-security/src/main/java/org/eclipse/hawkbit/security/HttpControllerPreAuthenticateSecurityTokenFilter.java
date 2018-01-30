/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 * {@code Example Header: Authorization: TargetToken
 * 5d8fSD54fdsFG98DDsa.}
 * 
 * The {@code Authorization} header is a HTTP standard and reverse proxy or
 * other proxies will keep the Authorization headers untouched instead of maybe
 * custom headers which have then weird side-effects. Furthermore frameworks are
 * aware of the sensitivity of the Authorization header and do not log it and
 * store it somewhere.
 *
 *
 *
 */
public class HttpControllerPreAuthenticateSecurityTokenFilter extends AbstractHttpControllerAuthenticationFilter {

    private final ControllerManagement controllerManagement;

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
    public HttpControllerPreAuthenticateSecurityTokenFilter(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final ControllerManagement controllerManagement, final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
        this.controllerManagement = controllerManagement;
    }

    @Override
    protected PreAuthenticationFilter createControllerAuthenticationFilter() {
        return new ControllerPreAuthenticateSecurityTokenFilter(tenantConfigurationManagement, controllerManagement,
                tenantAware, systemSecurityContext);
    }

}
