/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.event;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.transaction.annotation.Transactional;

/**
 * A TenantAwareEvent entity manager, which loads an entity by id and type for remote events.
 */
@Transactional(readOnly = true)
public class JpaEventEntityManager implements EventEntityManager {

    private final TenantAware tenantAware;
    private final EntityManager entityManager;

    /**
     * Constructor.
     *
     * @param tenantAware the tenant aware
     * @param entityManager the entity manager
     */
    public JpaEventEntityManager(final TenantAware tenantAware, final EntityManager entityManager) {
        this.tenantAware = tenantAware;
        this.entityManager = entityManager;
    }

    @Override
    public <E extends TenantAwareBaseEntity> E findEntity(final String tenant, final Long id, final Class<E> entityType) {
        return tenantAware.runAsTenant(tenant, () -> entityManager.find(entityType, id));
    }
}