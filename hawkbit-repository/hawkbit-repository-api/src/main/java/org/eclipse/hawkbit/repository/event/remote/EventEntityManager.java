/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * Loads an entity e.g. if a remote event is received.
 */
@FunctionalInterface
public interface EventEntityManager {

    /**
     * Find an entity by given id and return it.
     * 
     * @param tenant
     *            the tenant
     * @param id
     *            the id
     * @param entityType
     *            the entity type
     * @return the entity
     */
    <E extends TenantAwareBaseEntity> E findEntity(String tenant, Long id, Class<E> entityType);

}
