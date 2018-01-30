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
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;

/**
 * An pre-authenticated processing filter which add the
 * {@link SpringEvalExpressions#CONTROLLER_DOWNLOAD_ROLE_ANONYMOUS} to the
 * security context in case the anonymous download is allowed through
 * configuration.
 */
public class ControllerPreAuthenticatedAnonymousDownload extends AbstractControllerAuthenticationFilter {

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
    public ControllerPreAuthenticatedAnonymousDownload(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
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
    protected String getTenantConfigurationKey() {
        return TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED;
    }
}
