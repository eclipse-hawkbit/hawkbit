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
     * Counts all {@link SoftwareModule}s based on the given {@link Type}.
     *
     * @param type
     *            to count for
     * @return number of {@link SoftwareModule}s
     */
    Long countByType(JpaSoftwareModuleType type);

    /**
     * Retrieves {@link SoftwareModule} by filtering on name AND version AND
     * type (which is unique per tenant.
     * 
     * @param name
     *            to be filtered on
     * @param version
     *            to be filtered on
     * @param typeId
     *            to be filtered on
     * @return the found {@link SoftwareModule} with the given name AND version
     *         AND type
     */
    Optional<SoftwareModule> findOneByNameAndVersionAndTypeId(String name, String version, Long typeId);

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
            @Param("ids") final Long... ids);

    /**
     * @param pageable
     *            the page request to page the result set
     * @param setId
     *            to search for
     * @return all {@link SoftwareModule}s that are assigned to given
     *         {@link DistributionSet}.
     */
    Page<SoftwareModule> findByAssignedToId(Pageable pageable, Long setId);

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
     * retrieves all software modules with a given
     * {@link SoftwareModule#getId()}.
     *
     * @param ids
     *            to search for
     * @return {@link List} of found {@link SoftwareModule}s
     */
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT sm FROM JpaSoftwareModule sm WHERE sm.id IN ?1")
    List<JpaSoftwareModule> findByIdIn(Iterable<Long> ids);

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
