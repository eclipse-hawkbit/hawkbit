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
import javax.persistence.criteria.Selection;

import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A Event entity manager, which loads a entity for remote events.
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public class JpaEventEntityManager implements EventEntityManager {

    private final TenantAware tenantAware;

    @Autowired
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

    @SuppressWarnings("unchecked")
    @Override
    public <E extends TenantAwareBaseEntity> E findEntity(final String tenant, final Long id,
            final Class<? extends TenantAwareBaseEntity> entityType) {
        return (E) tenantAware.runAsTenant(tenant, () -> entityManager.find(entityType, id));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends TenantAwareBaseEntity> List<E> findEntities(final String tenant, final List<Long> ids,
            final Class<? extends TenantAwareBaseEntity> entityType) {

        return tenantAware.runAsTenant(tenant, () -> {

            final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<E> query = (CriteriaQuery<E>) builder.createQuery(entityType);
            final Root<AbstractJpaBaseEntity> root = (Root<AbstractJpaBaseEntity>) query.from(entityType);
            query.select((Selection<? extends E>) root);

            final Path<Long> path = root.get(AbstractJpaBaseEntity_.id);
            final Predicate in = path.in(ids);
            query.where(in);
            return entityManager.createQuery(query).getResultList();
        });
    }

}
