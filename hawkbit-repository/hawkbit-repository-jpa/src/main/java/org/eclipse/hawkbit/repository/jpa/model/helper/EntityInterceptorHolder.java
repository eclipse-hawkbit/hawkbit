/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.model.EntityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the {@link EntityInterceptor} to have all
 * interceptors in spring beans.
 * 
 */
public final class EntityInterceptorHolder {

    private static final EntityInterceptorHolder SINGLETON = new EntityInterceptorHolder();

    @Autowired(required = false)
    private final List<EntityInterceptor> entityInterceptors = new ArrayList<>();

    private EntityInterceptorHolder() {

    }

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
