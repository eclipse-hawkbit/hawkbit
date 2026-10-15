/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.SystemManagement;

/**
 * Static holder of the {@link SystemManagement} bean - gives access to it from places without injection (e.g. built in many configurations),
 * like the soft deleted tenant checks of the device facing APIs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // singleton holder ensures static access to spring resources in some places
public final class SystemManagementHolder {

    @SuppressWarnings("java:S3077") // we want to have just thread-safe ref
    private static volatile SystemManagement instance;

    // to be initialized by the SystemManagement creator with the fully initialized bean (proxy) in order to honor
    // things like @PreAuthorize, @Transactional, etc.
    public static void setInstance(final SystemManagement systemManagement) {
        SystemManagementHolder.instance = systemManagement;
    }

    /**
     * @return the {@link SystemManagement} bean
     * @throws IllegalStateException if not initialized
     */
    public static SystemManagement getInstance() {
        final SystemManagement systemManagement = instance;
        if (systemManagement == null) {
            throw new IllegalStateException("SystemManagement has not been initialized");
        }
        return systemManagement;
    }
}
