/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;

/**
 * An pre-authenticated processing filter which add the
 * {@link SpringEvalExpressions#CONTROLLER_DOWNLOAD_ROLE_ANONYMOUS} to the
 * security context in case the anonymous download is allowed through
 * configuration.
 */
public class HttpControllerPreAuthenticateAnonymousDownloadFilter extends AbstractHttpControllerAuthenticationFilter {

    /**
     * Constructor.
     * 
     * @param tenantConfigurationManagement
     *            the system management service to retrieve configuration
     *            properties
     * @param tenantAware
     *            the tenant aware service to get configuration for the specific
     *            tenant
     * @param systemSecurityContext
     *            the system security context
     */
    public HttpControllerPreAuthenticateAnonymousDownloadFilter(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
    }

    @Override
    protected PreAuthenticationFilter createControllerAuthenticationFilter() {
        return new ControllerPreAuthenticatedAnonymousDownload(tenantConfigurationManagement, tenantAware,
                systemSecurityContext);
    }

}
