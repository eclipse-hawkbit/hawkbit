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

import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds {@link SystemSecurityContext} service and makes
 * it accessible to beans which are not managed by spring, e.g. JPA entities.
 */
public final class SystemSecurityContextHolder {

    private static final SystemSecurityContextHolder INSTANCE = new SystemSecurityContextHolder();

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    private SystemSecurityContextHolder() {
    }

    /**
     * @return the singleton {@link SystemSecurityContextHolder} instance
     */
    public static SystemSecurityContextHolder getInstance() {
        return INSTANCE;
    }

    /**
     * @return the {@link SystemSecurityContext} service
     */
    public SystemSecurityContext getSystemSecurityContext() {
        return systemSecurityContext;
    }
}
