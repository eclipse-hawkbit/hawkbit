/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security.controller;

import java.util.Arrays;
import java.util.Collection;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * An abstraction for all controller based security. Check if the tenant
 * configuration is enabled.
 */
@Slf4j
public abstract class AbstractControllerAuthenticationFilter implements PreAuthenticationFilter {

    protected final TenantConfigurationManagement tenantConfigurationManagement;
    protected final TenantAware tenantAware;
    protected final SystemSecurityContext systemSecurityContext;
    private final SecurityConfigurationKeyTenantRunner configurationKeyTenantRunner;

    protected AbstractControllerAuthenticationFilter(
            final TenantConfigurationManagement systemManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        this.tenantConfigurationManagement = systemManagement;
        this.tenantAware = tenantAware;
        this.systemSecurityContext = systemSecurityContext;
        this.configurationKeyTenantRunner = new SecurityConfigurationKeyTenantRunner();
    }

    @Override
    public boolean isEnable(final ControllerSecurityToken securityToken) {
        return tenantAware.runAsTenant(securityToken.getTenant(), configurationKeyTenantRunner);
    }

    @Override
    public Collection<GrantedAuthority> getSuccessfulAuthenticationAuthorities() {
        return Arrays.asList(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    protected abstract String getTenantConfigurationKey();

    private final class SecurityConfigurationKeyTenantRunner implements TenantAware.TenantRunner<Boolean> {

        @Override
        public Boolean run() {

            log.trace("retrieving configuration value for configuration key {}", getTenantConfigurationKey());
            return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                    .getConfigurationValue(getTenantConfigurationKey(), Boolean.class).getValue());
        }

    }
}
