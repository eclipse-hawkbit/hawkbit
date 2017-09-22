/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@link SoftwareModuleType}.
 *
 */
@Transactional(readOnly = true)
public interface SoftwareModuleTypeRepository
        extends BaseEntityRepository<JpaSoftwareModuleType, Long>, JpaSpecificationExecutor<JpaSoftwareModuleType> {

    /**
     * @param pageable
     * @param isDeleted
     *            to <code>true</code> if only marked as deleted have to be
     *            count or all undeleted.
     * @return found {@link SoftwareModuleType}s.
     */
    Page<SoftwareModuleType> findByDeleted(Pageable pageable, boolean isDeleted);

    /**
     * @param isDeleted
     *            to <code>true</code> if only marked as deleted have to be
     *            count or all undeleted.
     * @return number of {@link SoftwareModuleType}s in the repository.
     */
    Long countByDeleted(boolean isDeleted);

    /**
     *
     * @param key
     *            to search for
     * @return all {@link SoftwareModuleType}s in the repository with given
     *         {@link SoftwareModuleType#getKey()}
     */
    Optional<SoftwareModuleType> findByKey(String key);

    /**
     *
     * @param name
     *            to search for
     * @return all {@link SoftwareModuleType}s in the repository with given
     *         {@link SoftwareModuleType#getName()}
     */
    Optional<SoftwareModuleType> findByName(String name);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant
     *            to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaSoftwareModuleType t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    @Override
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT d FROM JpaSoftwareModuleType d WHERE d.id IN ?1")
    List<JpaSoftwareModuleType> findAll(Iterable<Long> ids);
}
