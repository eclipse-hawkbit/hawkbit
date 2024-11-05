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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;

/**
 * Extract the {@code Authorization} header is a HTTP standard and reverse proxy
 * or other proxies will keep the Authorization headers untouched instead of
 * maybe custom headers which have then weird side-effects. Furthermore
 * frameworks are aware of the sensitivity of the Authorization header and do
 * not log it and store it somewhere.
 */
@Slf4j
public class HttpControllerPreAuthenticatedGatewaySecurityTokenFilter
        extends AbstractHttpControllerAuthenticationFilter {

    /**
     * Constructor.
     *
     * @param tenantConfigurationManagement the system management service to retrieve configuration
     *         properties
     * @param tenantAware the tenant aware service to get configuration for the specific
     *         tenant
     * @param systemSecurityContext the system security context
     */
    public HttpControllerPreAuthenticatedGatewaySecurityTokenFilter(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
    }

    @Override
    protected PreAuthenticationFilter createControllerAuthenticationFilter() {
        return new ControllerPreAuthenticatedGatewaySecurityTokenFilter(tenantConfigurationManagement, tenantAware,
                systemSecurityContext);
    }

    @Override
    protected Logger log() {
        return log;
    }
}