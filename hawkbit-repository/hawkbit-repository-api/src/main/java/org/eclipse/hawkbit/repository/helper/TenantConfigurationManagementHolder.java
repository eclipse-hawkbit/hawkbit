/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.helper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds {@link TenantConfigurationManagement} service and makes it accessible to beans which are
 * not managed by spring, e.g. JPA entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public final class TenantConfigurationManagementHolder {

    private static final TenantConfigurationManagementHolder SINGLETON = new TenantConfigurationManagementHolder();

    @Getter
    private TenantConfigurationManagement tenantConfigurationManagement;

    /**
     * @return the singleton {@link TenantConfigurationManagementHolder} instance
     */
    public static TenantConfigurationManagementHolder getInstance() {
        return SINGLETON;
    }

    @Autowired // spring setter injection
    public void setTenantConfigurationManagement(final TenantConfigurationManagement tenantConfigurationManagement) {
        this.tenantConfigurationManagement = tenantConfigurationManagement;
    }
}