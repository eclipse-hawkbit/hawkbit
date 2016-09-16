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

import javax.transaction.Transactional;

import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.support.Repositories;

/**
 *
 */
public class JpaEventEntityManager implements EventEntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaEventEntityManager.class);

    private final TenantAware tenantAware;
    private final Repositories repositories;

    /**
     * @param tenantAware
     * @param repositories
     */
    public JpaEventEntityManager(final TenantAware tenantAware, final Repositories repositories) {
        this.tenantAware = tenantAware;
        this.repositories = repositories;
    }

    @Override
    @Transactional
    public <E> E findEntity(final String tenant, final Long id, final Class<E> entityType) {
        return tenantAware.runAsTenant(tenant, () -> getRepositoryFor(entityType).findOne(id));
    }

    @Override
    @Transactional
    public <E> List<E> findEntities(final String tenant, final List<Long> ids, final Class<E> entityType) {
        return tenantAware.runAsTenant(tenant, () -> getRepositoryFor(entityType).findAll(ids));
    }

    @SuppressWarnings("unchecked")
    private <E> JpaRepository<E, Long> getRepositoryFor(final Class<E> entityType) {
        try {
            return (JpaRepository<E, Long>) getTargetObject(repositories.getRepositoryFor(entityType));
        } catch (final Exception e) {
            LOGGER.error("Could not get target JpaRepository from proxy", e);
            throw new IllegalStateException("Could not get parent JpaRepository", e);
        }
    }

    @SuppressWarnings({ "unchecked" })
    protected <T> T getTargetObject(final Object proxy) throws Exception {
        while (AopUtils.isJdkDynamicProxy(proxy)) {
            return (T) getTargetObject(((Advised) proxy).getTargetSource().getTarget());
        }
        return (T) proxy;
    }

}
