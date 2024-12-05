/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
     * @param tenant the tenant
     * @param id the id
     * @param entityType the entity type
     * @return the entity
     */
    <E extends TenantAwareBaseEntity> E findEntity(String tenant, Long id, Class<E> entityType);
}