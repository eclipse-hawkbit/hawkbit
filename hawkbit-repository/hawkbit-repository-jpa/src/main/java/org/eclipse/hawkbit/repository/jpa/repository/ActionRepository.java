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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.criteria.CriteriaBuilder;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Action} repository.
 */
@Transactional(readOnly = true)
public interface ActionRepository extends BaseEntityRepository<JpaAction> {

    /**
     * Retrieves an Action with all lazy attributes.
     *
     * @param actionId the ID of the action
     * @return the found {@link Action}
     */
    @EntityGraph(value = "Action.all", type = EntityGraphType.LOAD)
    Optional<Action> findWithDetailsById(@Param("id") Long actionId);

    /**
     * Retrieves the latest finished {@link Action} for given target and {@link DistributionSet}.
     *
     * @param targetId the action belongs to
     * @param dsId of the ds that is assigned to the target
     * @param status of the action
     * @return action if there is one with assigned target and assigned {@link DistributionSet}.
     */
    Optional<Action> findFirstByTargetIdAndDistributionSetIdAndStatusOrderByIdDesc(
            @Param("target") long targetId, @Param("ds") Long dsId, @Param("status") Action.Status status);

    /**
     * Switches the status of actions from one specific status into another, only if the actions are in a specific status. This should be
     * an atomic operation.
     * <p/>
     * No access control applied
     *
     * @param statusToSet the new status the actions should get
     * @param targetIds the IDs of the targets of the actions which are affected
     * @param active the active flag of the actions which should be affected
     * @param currentStatus the current status of the actions which are affected
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.status = :statusToSet WHERE a.target.id IN :targetsIds AND a.active = :active AND a.status = :currentStatus AND a.distributionSet.requiredMigrationStep = false")
    void switchStatus(
            @Param("statusToSet") Action.Status statusToSet, @Param("targetsIds") List<Long> targetIds,
            @Param("active") boolean active, @Param("currentStatus") Action.Status currentStatus);

    /**
     * Sets the given status on the given actions which are still active and not yet in that status (a concurrent change of the
     * action in between the read and this update is respected). Also increments the optimistic lock revision, explicitly, as a
     * JPQL bulk update doesn't increment it on every JPA vendor: a concurrent update based on the state read before then fails
     * with a concurrency failure instead of overwriting the status.
     * <p/>
     * No access control applied
     *
     * @param ids the IDs of the actions (at most {@code MAX_ENTRIES_IN_STATEMENT})
     * @param status the status to set
     * @param lastModifiedAt last modified at to set
     * @param lastModifiedBy last modified by to set
     * @return the number of updated actions
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.status = :status, a.lastModifiedAt = :lastModifiedAt, a.lastModifiedBy = :lastModifiedBy,"
            + " a.optLockRevision = a.optLockRevision + 1 WHERE a.id IN :ids AND a.active = true AND a.status <> :status")
    int setStatusOfActiveByIdsAndStatusNot(
            @Param("ids") Collection<Long> ids, @Param("status") Status status,
            @Param("lastModifiedAt") long lastModifiedAt, @Param("lastModifiedBy") String lastModifiedBy);

    /**
     * Like {@link #setStatusOfActiveByIdsAndStatusNot(Collection, Status, long, String)}, but sets the last action status code too.
     * <p/>
     * No access control applied
     *
     * @param ids the IDs of the actions (at most {@code MAX_ENTRIES_IN_STATEMENT})
     * @param status the status to set
     * @param lastActionStatusCode the last action status code to set
     * @param lastModifiedAt last modified at to set
     * @param lastModifiedBy last modified by to set
     * @return the number of updated actions
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.status = :status, a.lastActionStatusCode = :code, a.lastModifiedAt = :lastModifiedAt,"
            + " a.lastModifiedBy = :lastModifiedBy, a.optLockRevision = a.optLockRevision + 1"
            + " WHERE a.id IN :ids AND a.active = true AND a.status <> :status")
    int setStatusAndLastActionStatusCodeOfActiveByIdsAndStatusNot(
            @Param("ids") Collection<Long> ids, @Param("status") Status status, @Param("code") int lastActionStatusCode,
            @Param("lastModifiedAt") long lastModifiedAt, @Param("lastModifiedBy") String lastModifiedBy);

    /**
     * Counts all {@link Action}s referring to the given target.
     * <p/>
     * No access control applied
     *
     * @param controllerId the target to count the {@link Action}s
     * @return the count of actions referring to the given target
     */
    Long countByTargetControllerId(String controllerId);

    /**
     * Counts all {@link Action}s referring to the given targetId.
     * <p/>
     * No access control applied
     *
     * @param targetId the target to count the {@link Action}s
     * @return the count of actions referring to the given target
     */
    Long countByTargetId(Long targetId);

    /**
     * Counts the {@link Action}s per target for the given targets with one grouped query per chunk of
     * {@code MAX_ENTRIES_IN_STATEMENT} ids. Targets without actions are not in the result.
     * <p/>
     * No access control applied
     *
     * @param targetIds the targets to count the {@link Action}s of
     * @return target id to count of actions
     */
    default Map<Long, Long> countByTargetIds(final Collection<Long> targetIds) {
        final Map<Long, Long> counts = new HashMap<>();
        ListUtils.partition(List.copyOf(targetIds), Constants.MAX_ENTRIES_IN_STATEMENT).forEach(chunk ->
                countGroupedByTargetIdIn(chunk).forEach(row -> counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue())));
        return counts;
    }

    /**
     * Counts the {@link Action}s per target for the given targets (see {@link #countByTargetIds(Collection)}).
     * <p/>
     * No access control applied
     *
     * @param targetIds the targets (at most {@code MAX_ENTRIES_IN_STATEMENT})
     * @return rows of target id and count of actions
     */
    @Query("SELECT a.target.id, COUNT(a.id) FROM JpaAction a WHERE a.target.id IN :targetIds GROUP BY a.target.id")
    List<Object[]> countGroupedByTargetIdIn(@Param("targetIds") Collection<Long> targetIds);

    /**
     * Counts all {@link Action}s referring to the given DistributionSet.
     * <p/>
     * No access control applied
     *
     * @param distributionSet DistributionSet to count the {@link Action}s from
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetId(Long distributionSet);

    /**
     * Counts all active {@link Action}s referring to the given DistributionSet.
     * <p/>
     * No access control applied
     *
     * @param distributionSet DistributionSet to count the {@link Action}s from
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetIdAndActiveIsTrue(Long distributionSet);

    /**
     * Counts all active {@link Action}s referring to the given DistributionSet that are not in a given state.
     * <p/>
     * No access control applied
     *
     * @param distributionSet DistributionSet to count the {@link Action}s from
     * @param status the state the actions should not have
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetIdAndActiveIsTrueAndStatusIsNot(Long distributionSet, Status status);

    /**
     * Counts all actions referring to a given rollout and rolloutgroup which are currently not in the given status. An in-clause statement
     * does not work with the spring-data, so this is specific usecase regarding the rollout-management to find out actions which are not in
     * specific states.
     * <p/>
     * No access control applied
     *
     * @param rollout the rollout the actions are belong to
     * @param rolloutGroup the rollout group the actions are belong to
     * @param statuses the list of statuses the action should not have
     * @return the count of actions referring the rollout and rollout group and are not in given states
     */
    // TODO (Spring Boot 4 Migration): Eclipse link (5 beta) doesn't handle correctly the IN clause generated by Spring Data JPA
//    Long countByRolloutAndRolloutGroupAndStatusNotIn(JpaRollout rollout, JpaRolloutGroup rolloutGroup, List<Status> statuses);
    default Long countByRolloutAndRolloutGroupAndStatusNotIn(
            final JpaRollout rollout, final JpaRolloutGroup rolloutGroup, final List<Status> statuses) {
        return count((root, query, cb) -> {
            final CriteriaBuilder.In<Status> in = cb.in(root.get(JpaAction_.status));
            statuses.forEach(in::value);
            return cb.and(
                    cb.equal(root.get(JpaAction_.rollout).get(AbstractJpaBaseEntity_.id), rollout.getId()),
                    cb.equal(root.get(JpaAction_.rolloutGroup).get(AbstractJpaBaseEntity_.id), rolloutGroup.getId()),
                    cb.not(in));
        });
    }

    /**
     * Counts all actions referring to a given rollout and rollout group.
     * <p/>
     * No access control applied
     *
     * @param rollout the rollout the actions belong to
     * @param rolloutGroup the rollout group the actions belong to
     * @return the count of actions referring to a rollout and rollout group
     */
    Long countByRolloutAndRolloutGroup(JpaRollout rollout, JpaRolloutGroup rolloutGroup);

    /**
     * Counts all actions referring to a given rollout, rollout group and status.
     * <p/>
     * No access control applied
     *
     * @param rolloutId the ID of rollout the actions belong to
     * @param rolloutGroupId the ID rollout group the actions belong to
     * @param status the status the actions should have
     * @return the count of actions referring to a rollout, rollout group and are in a given status
     */
    Long countByRolloutIdAndRolloutGroupIdAndStatus(Long rolloutId, Long rolloutGroupId, Action.Status status);

    /**
     * Counts all actions referring to a given rollout and status.
     * <p/>
     * No access control applied
     *
     * @param rolloutId the ID of the rollout the actions belong to
     * @param status the status the actions should have
     * @return the count of actions referring to a rollout and are in a given
     *         status
     */
    Long countByRolloutIdAndStatus(Long rolloutId, Action.Status status);

    /**
     * Returns the number of active/non-active actions for a rollout
     *
     * @param rolloutId - the ID of the rollout the actions belong to
     * @param active - wether the actions should be active or not
     * @return number of actions which match the criteria
     */
    Long countByRolloutIdAndActive(Long rolloutId, boolean active);

    /**
     * Returns the number of active/non-active actions for a rollout which status is not the provided one.
     *
     * @param rolloutId - the ID of the rollout the actions belong to
     * @param active - wether the actions should be active or not
     * @param status - the status that matched actions should not be.
     * @return number of actions which match the criteria
     */
    Long countByRolloutIdAndActiveAndStatusNot(Long rolloutId, boolean active, Action.Status status);

    /**
     * Returns {@code true} if actions for the given rollout exists, otherwise {@code false}
     * <p/>
     * No access control applied
     *
     * @param rolloutId the ID of the rollout the actions belong to
     * @return {@code true} if actions for the given rollout exists, otherwise {@code false}
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaAction a WHERE a.rollout.id=:rolloutId")
    boolean existsByRolloutId(@Param("rolloutId") Long rolloutId);

    /**
     * Returns {@code true} if actions for the given rollout exists, otherwise {@code false}
     * <p/>
     * No access control applied
     *
     * @param rolloutId the ID of the rollout the actions belong to
     * @param status the action is not to be in
     * @return {@code true} if actions for the given rollout exists, otherwise {@code false}
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaAction a WHERE a.rollout.id=:rolloutId AND a.status != :status")
    boolean existsByRolloutIdAndStatusNot(@Param("rolloutId") Long rolloutId, @Param("status") Status status);

    /**
     * Returns the ids, greater than the given id, of up to {@code limit} actions of the given rollout groups in the given
     * status, ordered by id. No {@code COUNT} query is issued.
     * <p/>
     * No access control applied
     * <p/>
     * Note: it shall be JPA natively be implemented using entity graphs. However, Eclipslink does not make single call but N + 1.
     * If we somehow remove Eclipselink - we could go again on entity graphs (Hibernate is fine).
     *
     * @param rolloutGroupIds the IDs of the rollout groups
     * @param status the status the actions have
     * @param fromId exclusive lower bound of the ids (keyset paging)
     * @param limit maximum number of ids to return. Applied by Sprint Data applies the limit
     * @return the action ids
     */
    @Query("SELECT a.id FROM JpaAction a WHERE a.rolloutGroup.id IN :rolloutGroupIds AND a.status = :status AND a.id > :fromId ORDER BY a.id")
    List<Long> findIdsByRolloutGroupIdInAndStatusAndIdGreaterThan(
            @Param("rolloutGroupIds") Collection<Long> rolloutGroupIds,
            @Param("status") Status status,
            @Param("fromId") long fromId,
            Limit limit);

    /**
     * Returns the actions with the given ids, ordered by id, with their rollout group, target, the target's auto
     * confirmation status and assigned distribution set fetched in the same query. A plain {@code JOIN FETCH} by ids, as an
     * entity graph is not applied by every JPA vendor and a to-many fetch combined with paging is paged in memory.
     * <p/>
     * No access control applied
     * <p/>
     * Note: it shall be JPA natively be implemented using named graphs. However, Eclipslink does not make single call but N + 1.
     * If we somehow remove Eclipselink - we could go again on named graphs (Hibernate is fine).
     *
     * @param ids the IDs of the actions (at most {@code MAX_ENTRIES_IN_STATEMENT})
     * @return the actions
     */
    @Query("SELECT a FROM JpaAction a JOIN FETCH a.rolloutGroup JOIN FETCH a.target t LEFT JOIN FETCH t.autoConfirmationStatus"
            + " LEFT JOIN FETCH t.assignedDistributionSet WHERE a.id IN :ids ORDER BY a.id")
    List<JpaAction> findWithTargetByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * Retrieves all actions for a specific rollout and in a specific status.
     * <p/>
     * No access control applied
     *
     * @param pageable page parameters
     * @param rolloutId the rollout the actions belong to
     * @param actionStatus the status of the actions
     * @return the actions referring a specific rollout an in a specific status
     */
    Page<JpaAction> findByRolloutIdAndStatus(Pageable pageable, Long rolloutId, Status actionStatus);

    /**
     * Returns the ids of up to {@code limit} actions of the given rollout in the given status, ordered by id. No {@code COUNT}
     * query is issued.
     * <p/>
     * No access control applied
     *
     * @param rolloutId the ID of the rollout
     * @param status the status of the actions
     * @param limit maximum number of ids to return
     * @return the action ids
     */
    @Query("SELECT a.id FROM JpaAction a WHERE a.rollout.id = :rolloutId AND a.status = :status ORDER BY a.id")
    List<Long> findIdsByRolloutIdAndStatus(@Param("rolloutId") long rolloutId, @Param("status") Status status, Limit limit);

    /**
     * Deletes the given actions that are (still) in the given status with one bulk {@code DELETE}. The action status rows are
     * deleted by the database ({@code ON DELETE CASCADE}), so they are not loaded as for an entity remove. Bypasses the entity
     * callbacks - actions have no delete event.
     * <p/>
     * No access control applied
     *
     * @param ids the IDs of the actions (at most {@code MAX_ENTRIES_IN_STATEMENT})
     * @param status the status the actions must have
     * @return the number of deleted actions
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaAction a WHERE a.id IN :ids AND a.status = :status")
    int deleteByIdsAndStatus(@Param("ids") Collection<Long> ids, @Param("status") Status status);

    /**
     * Get list of objects which has details of status and count of targets in each status in specified rollout.
     * <p/>
     * No access control applied
     *
     * @param rolloutId id of {@link Rollout}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus(a.rollout.id, a.status, COUNT(a.id)) FROM JpaAction a WHERE a.rollout.id = ?1 GROUP BY a.rollout.id,a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutId(Long rolloutId);

    /**
     * Get list of objects which has details of status and count of targets in each status in specified rollout.
     * <p/>
     * No access control applied
     *
     * @param rolloutId id of {@link Rollout}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus(a.rollout.id, a.status, COUNT(a.id)) FROM JpaAction a WHERE a.rollout.id IN ?1 GROUP BY a.rollout.id,a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutIds(List<Long> rolloutId);

    /**
     * Get list of objects which has details of status and count of targets in each status in specified rollout group.
     * <p/>
     * No access control applied
     *
     * @param rolloutGroupId id of {@link RolloutGroup}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus(a.rolloutGroup.id, a.status, COUNT(a.id)) FROM JpaAction a WHERE a.rolloutGroup.id = ?1 GROUP BY a.rolloutGroup.id, a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutGroupId(Long rolloutGroupId);

    /**
     * Get list of objects which has details of status and count of targets in each status in specified rollout group.
     * <p/>
     * No access control applied
     *
     * @param rolloutGroupId list of id of {@link RolloutGroup}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus(a.rolloutGroup.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rolloutGroup.id IN ?1 GROUP BY a.rolloutGroup.id, a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutGroupIds(List<Long> rolloutGroupId);

    /**
     * Updates the externalRef of an action by its actionId.
     *
     * @param actionId for which the externalRef is being updated.
     * @param externalRef value of the external reference for the given action id.
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.externalRef = :externalRef WHERE a.id = :actionId")
    void updateExternalRef(@Param("actionId") Long actionId, @Param("externalRef") String externalRef);
}
