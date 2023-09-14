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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaStatistic;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DistributionSet} repository.
 *
 */
@Transactional(readOnly = true)
public interface DistributionSetRepository
        extends BaseEntityRepository<JpaDistributionSet, Long>, JpaSpecificationExecutor<JpaDistributionSet> {

    /**
     * Finds {@link DistributionSet}s by assigned {@link Tag}.
     * 
     * @param pageable
     *            paging and sorting information
     *
     * @param tagId
     *            to be found
     * @return list of found {@link DistributionSet}s
     */
    @Query(value = "Select Distinct ds from JpaDistributionSet ds join ds.tags dst where dst.id = :tag and ds.deleted = 0")
    Page<JpaDistributionSet> findByTag(Pageable pageable, @Param("tag") Long tagId);

    /**
     * Count {@link Rollout}s by Status for Distribution set.
     *
     * @param dsId
     *            to be found
     * @return map for {@link Rollout}s status counts
     */
    @Query(value = "SELECT r.status as name, COUNT(r.status) as data FROM JpaRollout r WHERE r.distributionSet.id = :dsId GROUP BY r.status")
    List<JpaStatistic> countRolloutsByStatusForDistributionSet(@Param("dsId") Long dsId);


    /**
     * Count {@link Action}s by Status for Distribution set.
     *
     * @param dsId
     *            to be found
     * @return map for {@link Action}s status counts
     */
    @Query(value = "SELECT a.status as name, COUNT(a.status) as data FROM JpaAction a WHERE a.distributionSet.id = :dsId GROUP BY a.status")
    List<JpaStatistic> countActionsByStatusForDistributionSet(@Param("dsId") Long dsId);

    /**
     * Count total AutoAssignments for Distribution set.
     *
     * @param dsId
     *            to be found
     * @return number of Auto Assignments for Distribution set
     */
    @Query(value = "SELECT COUNT(f.autoAssignDistributionSet) FROM JpaTargetFilterQuery f WHERE f.autoAssignDistributionSet.id = :dsId GROUP BY f.autoAssignDistributionSet")
    Long countAutoAssignmentsForDistributionSet(@Param("dsId") Long dsId);

    /**
     * deletes the {@link DistributionSet}s with the given IDs.
     * 
     * @param ids
     *            to be deleted
     */
    @Modifying
    @Transactional
    @Query("update JpaDistributionSet d set d.deleted = 1 where d.id in :ids")
    void deleteDistributionSet(@Param("ids") Long... ids);

    /**
     * deletes {@link DistributionSet}s by the given IDs.
     *
     * @param ids
     *            List of IDs of {@link DistributionSet}s to be deleted
     * @return number of affected/deleted records
     */
    @Modifying
    @Transactional
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("DELETE FROM JpaDistributionSet d WHERE d.id IN ?1")
    int deleteByIdIn(Collection<Long> ids);

    /**
     * Finds {@link DistributionSet}s where given {@link SoftwareModule} is
     * assigned.
     *
     * @param moduleId
     *            to search for
     * @return {@link List} of found {@link DistributionSet}s
     */
    Long countByModulesId(Long moduleId);

    /**
     * Finds {@link DistributionSet}s based on given ID that are assigned yet to
     * an {@link Action}, i.e. in use.
     *
     * @param ids
     *            to search for
     * @return list of {@link DistributionSet#getId()}
     */
    @Query("select ac.distributionSet.id from JpaAction ac where ac.distributionSet.id in :ids")
    List<Long> findAssignedToTargetDistributionSetsById(@Param("ids") Collection<Long> ids);

    /**
     * Finds {@link DistributionSet}s based on given ID that are assigned yet to
     * an {@link Rollout}, i.e. in use.
     *
     * @param ids
     *            to search for
     * @return list of {@link DistributionSet#getId()}
     */
    @Query("select ra.distributionSet.id from JpaRollout ra where ra.distributionSet.id in :ids")
    List<Long> findAssignedToRolloutDistributionSetsById(@Param("ids") Collection<Long> ids);

    /**
     * Finds the distribution set for a specific action.
     * 
     * @param action
     *            the action associated with the distribution set to find
     * @return the distribution set associated with the given action
     */
    @Query("select DISTINCT d from JpaDistributionSet d join fetch d.modules m join d.actions a where a.id = :action")
    JpaDistributionSet findByActionId(@Param("action") Long action);

    @Query("select DISTINCT ds from JpaDistributionSet ds join fetch ds.modules join ds.assignedToTargets t where t.controllerId = :controllerId")
    Optional<DistributionSet> findAssignedToTarget(@Param("controllerId") String controllerId);

    @Query("select DISTINCT ds from JpaDistributionSet ds join fetch ds.modules join ds.installedAtTargets t where t.controllerId = :controllerId")
    Optional<DistributionSet> findInstalledAtTarget(@Param("controllerId") String controllerId);

    /**
     * Counts {@link DistributionSet} instances of given type in the repository.
     *
     * @param typeId
     *            to search for
     * @return number of found {@link DistributionSet}s
     */
    long countByTypeId(Long typeId);

    /**
     * Counts {@link DistributionSet} with given
     * {@link DistributionSet#getName()} and
     * {@link DistributionSet#getVersion()}.
     *
     * @param name
     *            to search for
     * @param version
     *            to search for
     * @return number of found {@link DistributionSet}s
     */
    long countByNameAndVersion(String name, String version);

    @Override
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT d FROM JpaDistributionSet d WHERE d.id IN ?1")
    List<JpaDistributionSet> findAllById(Iterable<Long> ids);

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
    @Query("DELETE FROM JpaDistributionSet t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

}
