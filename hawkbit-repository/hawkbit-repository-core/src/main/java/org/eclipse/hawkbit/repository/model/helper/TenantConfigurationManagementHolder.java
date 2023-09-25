/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model.helper;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds {@link TenantConfigurationManagement} service
 * and makes it accessible to beans which are not managed by spring, e.g. JPA
 * entities.
 */
public final class TenantConfigurationManagementHolder {

    private static final TenantConfigurationManagementHolder INSTANCE = new TenantConfigurationManagementHolder();

    @Autowired
    private TenantConfigurationManagement tenantConfiguration;

    private TenantConfigurationManagementHolder() {
    }

    /**
     * @return the singleton {@link TenantConfigurationManagementHolder}
     *         instance
     */
    public static TenantConfigurationManagementHolder getInstance() {
        return INSTANCE;
    }

    /**
     * @return the {@link TenantConfigurationManagement} service
     */
    public TenantConfigurationManagement getTenantConfigurationManagement() {
        return tenantConfiguration;
    }

}
