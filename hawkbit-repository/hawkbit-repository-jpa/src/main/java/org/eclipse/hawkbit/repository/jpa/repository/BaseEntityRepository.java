/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.util.List;

import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaTenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

/**
 * Command repository operations for all {@link TenantAwareBaseEntity}s.
 *
 * @param <T>
 *            type if the entity type
 */
@NoRepositoryBean
@Transactional(readOnly = true)
public interface BaseEntityRepository<T extends AbstractJpaTenantAwareBaseEntity>
        extends PagingAndSortingRepository<T, Long>, CrudRepository<T, Long>,
                JpaSpecificationExecutor<T>, NoCountSliceRepository<T>, ACMRepository<T> {

    @Override
    List<T> findAllById(final Iterable<Long> ids);

    /**
     * Overrides
     * {@link org.springframework.data.repository.CrudRepository#saveAll(Iterable)}
     * to return a list of created entities instead of an instance of
     * {@link Iterable} to be able to work with it directly in further code
     * processing instead of converting the {@link Iterable}.
     *
     * @param entities to persist in the database
     * @return the created entities
     */
    @Override
    @Transactional
    <S extends T> List<S> saveAll(Iterable<S> entities);

    /**
     * Deletes all entities of a given tenant from this repository . For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will be done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    void deleteByTenant(String tenant);
}
