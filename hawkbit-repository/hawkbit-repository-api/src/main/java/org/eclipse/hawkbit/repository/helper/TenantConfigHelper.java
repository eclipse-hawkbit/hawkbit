/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.helper;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.USER_CONFIRMATION_ENABLED;

import java.io.Serializable;
import java.util.function.Function;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.context.SystemSecurityContext;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A collection of static helper methods for the tenant configuration
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TenantConfigHelper {

    private static final TenantConfigHelper SINGLETON = new TenantConfigHelper();

    @Getter
    private TenantConfigurationManagement tenantConfigurationManagement;

    @Autowired
    public void setTenantConfigurationManagement(final TenantConfigurationManagement tenantConfigurationManagement) {
        this.tenantConfigurationManagement = tenantConfigurationManagement;
    }

    public static TenantConfigHelper getInstance() {
        return SINGLETON;
    }

    public <T extends Serializable> T getConfigValue(final String key, final Class<T> valueType) {
        return SystemSecurityContext.runAsSystem(() -> tenantConfigurationManagement.getConfigurationValue(key, valueType).getValue());
    }

    /**
     * Is multi-assignments enabled for the current tenant
     *
     * @return is active
     */
    public boolean isMultiAssignmentsEnabled() {
        return getConfigValue(MULTI_ASSIGNMENTS_ENABLED, Boolean.class);
    }

    /**
     * Is confirmation flow enabled for the current tenant
     *
     * @return is enabled
     */
    public boolean isConfirmationFlowEnabled() {
        return getConfigValue(USER_CONFIRMATION_ENABLED, Boolean.class);
    }

    public Function<Target, PollStatus> pollStatusResolver() {
        return tenantConfigurationManagement.pollStatusResolver();
    }
}