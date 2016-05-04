/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
