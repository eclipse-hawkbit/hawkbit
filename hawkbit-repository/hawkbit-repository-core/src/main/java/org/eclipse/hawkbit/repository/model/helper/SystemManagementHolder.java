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

import org.eclipse.hawkbit.repository.SystemManagement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds {@link SystemManagement} service and makes it
 * accessible to beans which are not managed by spring, e.g. JPA entities.
 *
 *
 *
 **/
public final class SystemManagementHolder {

    private static final SystemManagementHolder INSTANCE = new SystemManagementHolder();

    @Autowired
    private SystemManagement systemManagement;

    private SystemManagementHolder() {
    }

    /**
     * @return the singleton {@link SystemManagementHolder} instance
     */
    public static SystemManagementHolder getInstance() {
        return INSTANCE;
    }

    /**
     * @return the systemManagement
     */
    public SystemManagement getSystemManagement() {
        return systemManagement;
    }

    /**
     * @param systemManagement
     *            the systemManagement to set
     */
    public void setSystemManagement(final SystemManagement systemManagement) {
        this.systemManagement = systemManagement;
    }

    /**
     * @return the {@link SystemManagement#currentTenant()}.
     */
    public String currentTenant() {
        return systemManagement.currentTenant();
    }

}
