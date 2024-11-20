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

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring data repositories for {@link TargetFilterQuery}s.
 */
@Transactional(readOnly = true)
public interface TargetFilterQueryRepository
        extends BaseEntityRepository<JpaTargetFilterQuery> {

    /**
     * Find customer target filter by name
     *
     * @param name
     * @return custom target filter
     */
    Optional<TargetFilterQuery> findByName(String name);

    /**
     * Sets the auto assign distribution sets and action types to null which
     * match the ds ids.
     * <p/>
     * No access control applied
     *
     * @param dsIds distribution set ids to be set to null
     */
    @Modifying
    @Transactional
    @Query("update JpaTargetFilterQuery d set d.autoAssignDistributionSet = NULL, d.autoAssignActionType = NULL, d.accessControlContext = NULL where d.autoAssignDistributionSet in :ids")
    void unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(@Param("ids") Long... dsIds);

    /**
     * Counts all target filters that have a given auto assign distribution set
     * assigned.
     * <p/>
     * No access control applied
     *
     * @param autoAssignDistributionSetId the id of the distribution set
     * @return the count
     */
    long countByAutoAssignDistributionSetId(long autoAssignDistributionSetId);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaTargetFilterQuery t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}
