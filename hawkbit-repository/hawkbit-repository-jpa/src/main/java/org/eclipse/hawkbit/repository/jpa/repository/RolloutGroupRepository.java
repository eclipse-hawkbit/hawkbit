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

import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * The repository interface for the {@link RolloutGroup} model.
 */
@Transactional(readOnly = true)
public interface RolloutGroupRepository extends BaseEntityRepository<JpaRolloutGroup> {

    /**
     * Retrieves all {@link RolloutGroup} referring a specific rollout in the order of creating them. ID ASC.
     *
     * @param rollout the rollout the rolloutgroups belong to
     * @return the rollout groups belonging to a rollout ordered by ID ASC.
     */
    List<JpaRolloutGroup> findByRolloutOrderByIdAsc(JpaRollout rollout);

    /**
     * Retrieves all {@link RolloutGroup} referring a specific rollout in a specific {@link RolloutGroupStatus}.
     *
     * @param rollout the rollout the rolloutgroup belong to
     * @param status the status of the rollout groups
     * @return the rollout groups belonging to a rollout in a specific status
     */
    List<JpaRolloutGroup> findByRolloutAndStatus(Rollout rollout, RolloutGroupStatus status);

    /**
     * Retrieves all {@link RolloutGroup} for a specific parent in a specific status. Retrieves the child rolloutgroup for a specific status.
     *
     * @param rolloutGroupId the rolloutgroupId to find the parents
     * @param status the status of the rolloutgroups
     * @return The child {@link RolloutGroup}s in a specific status
     */
    @Query("SELECT g FROM JpaRolloutGroup g WHERE g.parent.id=:rollout_group and g.status=:status")
    List<JpaRolloutGroup> findByParentIdAndStatus(@Param("rollout_group") long rolloutGroupId, @Param("status") RolloutGroupStatus status);

    /**
     * Updates all {@link RolloutGroup#getStatus()} of children for given parent.
     *
     * @param parent the parent rolloutgroup
     * @param status the status of the rolloutgroups
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaRolloutGroup g SET g.status = :status WHERE g.parent = :parent")
    void setStatusForChildren(@Param("status") RolloutGroupStatus status, @Param("parent") RolloutGroup parent);

    /**
     * Retrieves all {@link RolloutGroup}s for a specific rollout and status not having.
     *
     * @param rollout the rollout the rolloutgroup belong to
     * @param status the status which the rolloutgroup should not have
     * @return rolloutgroup referring to a rollout and not having a specific status.
     */
    List<JpaRolloutGroup> findByRolloutAndStatusNotIn(JpaRollout rollout, Collection<RolloutGroupStatus> status);

    /**
     * Retrieves all {@link RolloutGroup} for a specific rollout.
     *
     * @param rolloutId the ID of the rollout to find the rollout groups
     * @param page the page request to sort, limit the result
     * @return a page of found {@link RolloutGroup} or {@code empty}.
     */
    Page<JpaRolloutGroup> findByRolloutId(Long rolloutId, Pageable page);

    /**
     * Counts all {@link RolloutGroup} for a specific rollout.
     *
     * @param rolloutId the ID of the rollout to find the rollout groups
     * @return the amount of found {@link RolloutGroup}s.
     */
    long countByRolloutId(Long rolloutId);

    /**
     * Counts all {@link RolloutGroup} referring a specific rollout in specific {@link RolloutGroupStatus}s. An in-clause statement
     * does not work with the spring-data, so this is specific usecase regarding the rollout-management to find out rolloutgroups which are
     * in specific states.
     *
     * @param rolloutId the ID of the rollout the rolloutgroup belong to
     * @param rolloutGroupStatus1 the status of the rollout groups
     * @param rolloutGroupStatus2 the status of the rollout groups
     * @return the count of rollout groups belonging to a rollout in specific status
     */
    @Query("SELECT COUNT(r.id) FROM JpaRolloutGroup r WHERE r.rollout.id = :rolloutId and (r.status = :status1 or r.status = :status2)")
    Long countByRolloutIdAndStatusOrStatus(
            @Param("rolloutId") long rolloutId,
            @Param("status1") RolloutGroupStatus rolloutGroupStatus1,
            @Param("status2") RolloutGroupStatus rolloutGroupStatus2);
}