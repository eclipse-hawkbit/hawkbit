/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
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
 * {@link SoftwareModule} repository.
 *
 */
@Transactional(readOnly = true)
public interface SoftwareModuleRepository
        extends BaseEntityRepository<JpaSoftwareModule, Long>, JpaSpecificationExecutor<JpaSoftwareModule> {

    /**
     * Counts all {@link SoftwareModule}s based on the given {@link JpaSoftwareModuleType}.
     *
     * @param type
     *            to count for
     * @return number of {@link SoftwareModule}s
     */
    Long countByType(JpaSoftwareModuleType type);

    /**
     * deletes the {@link SoftwareModule}s with the given IDs.
     * 
     * @param modifiedAt
     *            current timestamp
     * @param modifiedBy
     *            user name of current auditor
     * @param ids
     *            to be deleted
     *
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaSoftwareModule b SET b.deleted = 1, b.lastModifiedAt = :lastModifiedAt, b.lastModifiedBy = :lastModifiedBy WHERE b.id IN :ids")
    void deleteSoftwareModule(@Param("lastModifiedAt") Long modifiedAt, @Param("lastModifiedBy") String modifiedBy,
            @Param("ids") Long... ids);


    /**
     * Count the software modules which are assigned to the distribution set
     * with the given ID.
     * 
     * @param setId
     *            the distribution set ID
     * 
     * @return the number of software modules matching the given distribution
     *         set ID.
     */
    long countByAssignedToId(Long setId);

    /**
     * @param pageable
     *            the page request to page the result set
     * @param set
     *            to search for
     * @param type
     *            to filter
     * @return all {@link SoftwareModule}s that are assigned to given
     *         {@link DistributionSet} filtered by {@link SoftwareModuleType}.
     */
    Page<SoftwareModule> findByAssignedToAndType(Pageable pageable, JpaDistributionSet set, SoftwareModuleType type);

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
    @Query("DELETE FROM JpaSoftwareModule t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}
