/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.eclipse.hawkbit.dmf.json.model.DmfTenantSecurityToken;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstraction for all controller based security. Check if the tenant
 * configuration is enabled.
 *
 *
 *
 */
public abstract class AbstractControllerAuthenticationFilter implements PreAuthentificationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractControllerAuthenticationFilter.class);

    protected final TenantConfigurationManagement tenantConfigurationManagement;
    protected final TenantAware tenantAware;
    private final SecurityConfigurationKeyTenantRunner configurationKeyTenantRunner;
    protected final SystemSecurityContext systemSecurityContext;

    protected AbstractControllerAuthenticationFilter(final TenantConfigurationManagement systemManagement,
            final TenantAware tenantAware, final SystemSecurityContext systemSecurityContext) {
        this.tenantConfigurationManagement = systemManagement;
        this.tenantAware = tenantAware;
        this.systemSecurityContext = systemSecurityContext;
        this.configurationKeyTenantRunner = new SecurityConfigurationKeyTenantRunner();
    }

    protected abstract String getTenantConfigurationKey();

    @Override
    public boolean isEnable(final DmfTenantSecurityToken secruityToken) {
        return tenantAware.runAsTenant(secruityToken.getTenant(), configurationKeyTenantRunner);
    }

    private final class SecurityConfigurationKeyTenantRunner implements TenantAware.TenantRunner<Boolean> {
        @Override
        public Boolean run() {

            LOGGER.trace("retrieving configuration value for configuration key {}", getTenantConfigurationKey());
            return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                    .getConfigurationValue(getTenantConfigurationKey(), Boolean.class).getValue());
        }

    }
}
