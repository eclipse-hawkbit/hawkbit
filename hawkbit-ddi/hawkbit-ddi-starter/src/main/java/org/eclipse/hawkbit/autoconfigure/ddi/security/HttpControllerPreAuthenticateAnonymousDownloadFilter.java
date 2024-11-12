/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.ddi.security;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.ControllerPreAuthenticatedAnonymousDownload;
import org.eclipse.hawkbit.security.PreAuthenticationFilter;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;

/**
 * An pre-authenticated processing filter which add the
 * {@link SpringEvalExpressions#CONTROLLER_DOWNLOAD_ROLE_ANONYMOUS} to the
 * security context in case the anonymous download is allowed through
 * configuration.
 */
@Slf4j
public class HttpControllerPreAuthenticateAnonymousDownloadFilter extends AbstractHttpControllerAuthenticationFilter {

    /**
     * Constructor.
     *
     * @param tenantConfigurationManagement the system management service to retrieve configuration properties
     * @param tenantAware the tenant aware service to get configuration for the specific tenant
     * @param systemSecurityContext the system security context
     */
    public HttpControllerPreAuthenticateAnonymousDownloadFilter(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
    }

    @Override
    protected PreAuthenticationFilter createControllerAuthenticationFilter() {
        return new ControllerPreAuthenticatedAnonymousDownload(tenantConfigurationManagement, tenantAware, systemSecurityContext);
    }

    @Override
    protected Logger log() {
        return log;
    }
}