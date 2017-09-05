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

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PagingAndSortingRepository} for {@link DistributionSetType}.
 *
 */
@Transactional(readOnly = true)
public interface DistributionSetTypeRepository
        extends BaseEntityRepository<JpaDistributionSetType, Long>, JpaSpecificationExecutor<JpaDistributionSetType> {

    /**
     *
     * @param pageable
     *            page parameters
     * @param isDeleted
     *            to <code>true</code> if only soft deleted entries of
     *            <code>false</code> if undeleted ones
     * @return list of found {@link DistributionSetType}s
     */
    Page<JpaDistributionSetType> findByDeleted(Pageable pageable, boolean isDeleted);

    /**
     * @param isDeleted
     *            to <code>true</code> if only marked as deleted have to be
     *            count or all undeleted.
     * @return number of {@link DistributionSetType}s in the repository.
     */
    long countByDeleted(boolean isDeleted);

    /**
     * Counts all distribution set type where a specific software module type is
     * assigned to.
     * 
     * @param softwareModuleType
     *            the software module type to count the distribution set type
     *            which has this software module type assigned
     * @return the number of {@link DistributionSetType}s in the repository
     *         assigned to the given software module type
     */
    long countByElementsSmType(JpaSoftwareModuleType softwareModuleType);

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
    @Query("DELETE FROM JpaDistributionSetType t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    @Override
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT d FROM JpaDistributionSetType d WHERE d.id IN ?1")
    List<JpaDistributionSetType> findAll(Iterable<Long> ids);
}
