/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds {@link SystemManagement} service and makes it
 * accessible to beans which are not managed by spring, e.g. JPA entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SystemManagementHolder {

    private static final SystemManagementHolder INSTANCE = new SystemManagementHolder();

    @Getter
    @Setter
    @Autowired
    private SystemManagement systemManagement;

    /**
     * @return the singleton {@link SystemManagementHolder} instance
     */
    public static SystemManagementHolder getInstance() {
        return INSTANCE;
    }
}