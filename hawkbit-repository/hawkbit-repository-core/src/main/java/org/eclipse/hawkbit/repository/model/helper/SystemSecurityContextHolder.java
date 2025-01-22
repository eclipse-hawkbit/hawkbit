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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds {@link SystemSecurityContext} service and makes it accessible to beans which are not
 * managed by spring, e.g. JPA entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public final class SystemSecurityContextHolder {

    private static final SystemSecurityContextHolder SINGLETON = new SystemSecurityContextHolder();

    @Getter
    private SystemSecurityContext systemSecurityContext;

    /**
     * @return the singleton {@link SystemSecurityContextHolder} instance
     */
    public static SystemSecurityContextHolder getInstance() {
        return SINGLETON;
    }

    @Autowired // spring setter injection
    public void setSystemSecurityContext(final SystemSecurityContext systemSecurityContext) {
        this.systemSecurityContext = systemSecurityContext;
    }
}