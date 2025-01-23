/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.EntityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the {@link EntityInterceptor} to have all interceptors in spring beans.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public final class EntityInterceptorHolder {

    private static final EntityInterceptorHolder SINGLETON = new EntityInterceptorHolder();

    @Autowired(required = false)
    private final List<EntityInterceptor> entityInterceptors = new ArrayList<>();

    /**
     * @return the entity intreceptor holder singleton instance
     */
    public static EntityInterceptorHolder getInstance() {
        return SINGLETON;
    }

    public List<EntityInterceptor> getEntityInterceptors() {
        return entityInterceptors;
    }
}