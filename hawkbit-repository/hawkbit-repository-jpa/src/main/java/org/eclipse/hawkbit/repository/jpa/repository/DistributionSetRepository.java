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

import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaStatistic;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DistributionSet} repository.
 */
@Transactional(readOnly = true)
public interface DistributionSetRepository extends BaseEntityRepository<JpaDistributionSet> {

    /**
     * Count {@link Rollout}s by Status for Distribution set.
     * <p/>
     * No access control applied.
     *
     * @param dsId to be found
     * @return map for {@link Rollout}s status counts
     */
    @Query(value = "SELECT r.status as name, COUNT(r.status) as data FROM JpaRollout r WHERE r.distributionSet.id = :dsId GROUP BY r.status")
    List<JpaStatistic> countRolloutsByStatusForDistributionSet(@Param("dsId") Long dsId);

    /**
     * Count {@link Action}s by Status for Distribution set.
     * <p/>
     * No access control applied.
     *
     * @param dsId to be found
     * @return map for {@link Action}s status counts
     */
    @Query(value = "SELECT a.status as name, COUNT(a.status) as data FROM JpaAction a WHERE a.distributionSet.id = :dsId GROUP BY a.status")
    List<JpaStatistic> countActionsByStatusForDistributionSet(@Param("dsId") Long dsId);

    /**
     * Count total AutoAssignments for Distribution set.
     * <p/>
     * No access control applied.
     *
     * @param dsId to be found
     * @return number of Auto Assignments for Distribution set
     */
    @Query(value = "SELECT COUNT(f.autoAssignDistributionSet) FROM JpaTargetFilterQuery f WHERE f.autoAssignDistributionSet.id = :dsId GROUP BY f.autoAssignDistributionSet")
    Long countAutoAssignmentsForDistributionSet(@Param("dsId") Long dsId);

    /**
     * Finds {@link DistributionSet}s based on given ID that are assigned yet to an {@link Action}, i.e. in use.
     * <p/>
     * No access control applied.
     *
     * @param ids to search for
     * @return list of {@link DistributionSet#getId()}
     */
    @Query("SELECT ac.distributionSet.id FROM JpaAction ac WHERE ac.distributionSet.id IN :ids")
    List<Long> findAssignedToTargetDistributionSetsById(@Param("ids") Collection<Long> ids);

    /**
     * Finds {@link DistributionSet}s based on given ID that are assigned yet to an {@link Rollout}, i.e. in use.
     * <p/>
     * No access control applied.
     *
     * @param ids to search for
     * @return list of {@link DistributionSet#getId()}
     */
    @Query("SELECT ra.distributionSet.id FROM JpaRollout ra WHERE ra.distributionSet.id IN :ids")
    List<Long> findAssignedToRolloutDistributionSetsById(@Param("ids") Collection<Long> ids);

    /**
     * Counts {@link DistributionSet} instances of given type in the repository.
     * <p/>
     * No access control applied.
     *
     * @param typeId to search for
     * @return number of found {@link DistributionSet}s
     */
    long countByTypeId(Long typeId);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety reasons (this is a "delete everything" query after all) we add
     * the tenant manually to query even if this will by done by {@link EntityManager} anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaDistributionSet t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}