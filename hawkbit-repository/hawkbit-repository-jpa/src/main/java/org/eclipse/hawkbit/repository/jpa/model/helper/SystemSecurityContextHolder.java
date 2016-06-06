/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

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
