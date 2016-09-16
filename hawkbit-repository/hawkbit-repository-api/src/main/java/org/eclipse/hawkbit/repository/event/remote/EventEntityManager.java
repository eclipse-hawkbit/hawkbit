/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.List;

/**
 * Loads a entity e.g. if a remote event is received.
 */
public interface EventEntityManager {

    /**
     * Find a entity by given id.
     * 
     * @param tenant
     *            the tenant
     * @param id
     *            the id
     * @param entityType
     *            the entity type
     * @return the entity
     */
    <E> E findEntity(String tenant, Long id, Class<E> entityType);

    /**
     * Finds all entities by given ids.
     * 
     * @param tenant
     *            the tenant
     * @param ids
     *            the id
     * @param entityType
     *            the entity type
     * @return the entities
     */
    <E> List<E> findEntities(String tenant, List<Long> ids, Class<E> entityType);
}
