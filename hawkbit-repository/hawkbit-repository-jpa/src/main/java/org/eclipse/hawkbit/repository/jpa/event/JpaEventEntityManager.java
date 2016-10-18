/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.event;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaTenantAwareBaseEntity_;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A TenantAwareEvent entity manager, which loads an entity by id and type for
 * remote events.
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public class JpaEventEntityManager implements EventEntityManager {

    private final TenantAware tenantAware;

    private final EntityManager entityManager;

    /**
     * Constructor.
     *
     * @param tenantAware
     *            the tenant aware
     * @param entityManager
     *            the entity manager
     */
    public JpaEventEntityManager(final TenantAware tenantAware, final EntityManager entityManager) {
        this.tenantAware = tenantAware;
        this.entityManager = entityManager;
    }

    @Override
    public <E extends TenantAwareBaseEntity> E findEntity(final String tenant, final Long id,
            final Class<E> entityType) {
        return tenantAware.runAsTenant(tenant, () -> {
            final E entity = entityManager.find(entityType, id);
            // The entityManager needs to refresh the entity so the new entity
            // value is available on other nodes. It is necessary to read the
            // entity from the database again to ensure there are no stale data
            // in the cache.
            entityManager.refresh(entity);
            return entity;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends TenantAwareBaseEntity> List<E> findEntities(final String tenant, final List<Long> ids,
            final Class<E> entityType) {

        return tenantAware.runAsTenant(tenant, () -> {

            final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<E> query = builder.createQuery(entityType);
            final Root<E> root = query.from(entityType);
            query.select(root);

            final Path<Long> path = root.get((SingularAttribute<? super E, Long>) AbstractJpaTenantAwareBaseEntity_.id);
            final Predicate in = path.in(ids);
            query.where(in);
            return entityManager.createQuery(query).getResultList();
        });
    }

}
