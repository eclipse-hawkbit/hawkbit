/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Action} repository.
 *
 */
@Transactional(readOnly = true)
public interface ActionRepository extends BaseEntityRepository<JpaAction, Long>, JpaSpecificationExecutor<JpaAction> {
    /**
     * Retrieves an Action with all lazy attributes.
     *
     * @param actionId
     *            the ID of the action
     * @return the found {@link Action}
     */
    @EntityGraph(value = "Action.all", type = EntityGraphType.LOAD)
    Optional<Action> getById(Long actionId);

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link DistributionSet}.
     *
     * @param pageable
     *            page parameters
     * @param dsId
     *            the {@link DistributionSet} on which will be filtered
     * @return the found {@link Action}s
     */
    Page<Action> findByDistributionSetId(Pageable pageable, Long dsId);

    /**
     * Retrieves all active {@link Action}s which are referring the given
     * {@link DistributionSet}.
     *
     * @param set
     *            the {@link DistributionSet} on which will be filtered
     * @return the found {@link Action}s
     */
    List<Action> findByDistributionSetAndActiveIsTrue(DistributionSet set);

    /**
     * Retrieves all active {@link Action}s which are referring the given
     * {@link DistributionSet} and are not in the given state
     *
     * @param set
     *            the {@link DistributionSet} on which will be filtered
     * @param status
     *            the state the actions should not have
     * @return the found {@link Action}s
     */
    List<Action> findByDistributionSetAndActiveIsTrueAndStatusIsNot(DistributionSet set, Status status);

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link Target}.
     *
     * @param pageable
     *            page parameters
     * @param controllerId
     *            the target to find assigned actions
     * @return the found {@link Action}s
     */
    Slice<Action> findByTargetControllerId(Pageable pageable, String controllerId);

    /**
     * Retrieves all {@link Action}s which are referring the given targetId
     *
     * @param pageable
     *            page parameters
     * @param targetId
     *            the target to find assigned actions for
     * @return the found {@link Action}s
     */
    Page<Action> findByTargetId(Pageable pageable, Long targetId);

    /**
     * Retrieves all {@link Action}s which are active and referring to the given
     * {@link Target} order by ID ascending.
     *
     * @param target
     *            the target to find assigned actions
     * @param active
     *            the action active flag
     * @return the found {@link Action}s
     */
    List<Action> findByTargetAndActiveOrderByIdAsc(JpaTarget target, boolean active);

    /**
     * Retrieves the active {@link Action}s with the highest weights that refer
     * to the given {@link Target}. If {@link Action}s have the same weight they
     * are ordered ascending by ID (oldest ones first).
     *
     * @param pageable
     *            pageable
     * @param controllerId
     *            the target to find assigned actions
     * @return the found {@link Action}s
     */
    @EntityGraph(value = "Action.ds", type = EntityGraphType.LOAD)
    Page<Action> findByTargetControllerIdAndActiveIsTrueAndWeightIsNotNullOrderByWeightDescIdAsc(Pageable pageable,
            String controllerId);

    /**
     * Retrieves the active {@link Action}s with the lowest IDs (the oldest one)
     * whose weight is null and that that refers to the given {@link Target}.
     *
     * @param pageable
     *            pageable
     * @param controllerId
     *            the target to find assigned actions
     * @return the found {@link Action}s
     */
    @EntityGraph(value = "Action.ds", type = EntityGraphType.LOAD)
    Page<Action> findByTargetControllerIdAndActiveIsTrueAndWeightIsNullOrderByIdAsc(Pageable pageable,
            String controllerId);

    /**
     * Checks if an active action exists for given
     * {@link Target#getControllerId()}.
     *
     * @param controllerId
     *            of target to check
     * @return <code>true</code> if an active action for the target exists.
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaAction a JOIN a.target t WHERE t.controllerId=:controllerId AND a.active=1")
    boolean activeActionExistsForControllerId(@Param("controllerId") String controllerId);

    /**
     * Check if any active actions with given action status and given controller
     * ID exist.
     *
     * @param controllerId
     *            of the target to check for actions
     * @param currentStatus
     *            of the active action to look for
     *
     * @return <code>true</code> if one or more active actions for the given
     *         controllerId and action status are found
     */
    boolean existsByTargetControllerIdAndStatusAndActiveIsTrue(String controllerId, Action.Status currentStatus);

    /**
     * Retrieves latest {@link Action} for given target and
     * {@link SoftwareModule}.
     *
     * @param targetId
     *            to search for
     * @param moduleId
     *            to search for
     * @return action if there is one with assigned target and module is part of
     *         assigned {@link DistributionSet}.
     */
    @Query("Select a from JpaAction a join a.distributionSet ds join ds.modules modul where a.target.controllerId = :target and modul.id = :module order by a.id desc")
    List<Action> findActionByTargetAndSoftwareModule(@Param("target") String targetId, @Param("module") Long moduleId);

    /**
     * Retrieves the latest finished {@link Action} for given target and {@link DistributionSet}.
     *
     * @param targetId
     *            the action belongs to
     * @param dsId
     *            of the ds that is assigned to the target
     * @param status
     *            of the action
     * @return action if there is one with assigned target and assigned
     *         {@link DistributionSet}.
     */
    Optional<Action> findFirstByTargetIdAndDistributionSetIdAndStatusOrderByIdDesc(@Param("target") long targetId,
            @Param("ds") Long dsId, @Param("status") Action.Status status);

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link DistributionSet} and {@link Target}.
     *
     * @param pageable
     *            page parameters
     * @param target
     *            is the assigned target
     * @param ds
     *            the {@link DistributionSet} on which will be filtered
     * @return the found {@link Action}s
     */
    @Query("Select a from JpaAction a where a.target = :target and a.distributionSet = :ds")
    Page<JpaAction> findByTargetAndDistributionSet(Pageable pageable, @Param("target") JpaTarget target,
            @Param("ds") JpaDistributionSet ds);

    /**
     * Retrieves all {@link Action}s of a specific target, without pagination
     * ordered by action ID.
     *
     * @param target
     *            to search for
     * @return a list of actions according to the searched target
     */
    @Query("Select a from JpaAction a where a.target = :target order by a.id")
    List<JpaAction> findByTarget(@Param("target") Target target);

    /**
     * Retrieves all {@link Action}s of a specific target and given active flag
     * ordered by action ID. Loads also the lazy {@link Action#getDistributionSet()}
     * field.
     *
     * @param pageable
     *            page parameters
     * @param controllerId
     *            to search for
     * @param active
     *            {@code true} for all actions which are currently active,
     *            {@code false} for inactive
     * @return a list of actions
     */
    @EntityGraph(value = "Action.ds", type = EntityGraphType.LOAD)
    @Query("Select a from JpaAction a where a.target.controllerId = :controllerId and a.active = :active")
    Page<Action> findByActiveAndTarget(Pageable pageable, @Param("controllerId") String controllerId,
            @Param("active") boolean active);

    /**
     * Switches the status of actions from one specific status into another, only if
     * the actions are in a specific status. This should be a atomar operation.
     *
     * @param statusToSet
     *            the new status the actions should get
     * @param targetIds
     *            the IDs of the targets of the actions which are affected
     * @param active
     *            the active flag of the actions which should be affected
     * @param currentStatus
     *            the current status of the actions which are affected
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.status = :statusToSet WHERE a.target IN :targetsIds AND a.active = :active AND a.status = :currentStatus AND a.distributionSet.requiredMigrationStep = false")
    void switchStatus(@Param("statusToSet") Action.Status statusToSet, @Param("targetsIds") List<Long> targetIds,
            @Param("active") boolean active, @Param("currentStatus") Action.Status currentStatus);

    /**
     *
     * Retrieves all active {@link Action}s by given controllerId filtered by a
     * status
     *
     * @param controllerId
     *            the IDs of targets for the actions
     * @param status
     *            the current status of the actions
     * @return the found list of {@link Action}
     */
    @Query("SELECT a FROM JpaAction a WHERE a.target.controllerId = :controllerId AND a.active = true AND a.status = :status")
    List<JpaAction> findByTargetIdAndIsActiveAndActionStatus(@Param("controllerId") String controllerId,
            @Param("status") Action.Status status);

    /**
     *
     * Retrieves all IDs for {@link Action}s referring to the given target IDs,
     * active flag, current status and distribution set not requiring migration
     * step.
     *
     * @param targetIds
     *            the IDs of targets for the actions
     * @param active
     *            flag to indicate active/inactive actions
     * @param currentStatus
     *            the current status of the actions
     * @return the found list of {@link Action} IDs
     */
    @Query("SELECT a.id FROM JpaAction a WHERE a.target IN :targetsIds AND a.active = :active AND a.status = :currentStatus AND a.distributionSet.requiredMigrationStep = false")
    List<Long> findByTargetIdInAndIsActiveAndActionStatusAndDistributionSetNotRequiredMigrationStep(
            @Param("targetsIds") List<Long> targetIds, @Param("active") boolean active,
            @Param("currentStatus") Action.Status currentStatus);

    /**
     * Retrieves all {@link Action}s that matches the queried externalRefs.
     *
     * @param externalRefs
     *            for which the actions need to be found
     * @param active
     *            flag to indicate active/inactive actions
     * @return list of actions
     */
    List<Action> findByExternalRefInAndActive(@Param("externalRefs") List<String> externalRefs,
            @Param("active") boolean active);

    /**
     * Retrieves an {@link Action} that matches the queried externalRef.
     *
     * @param externalRef
     *            of the action. See {@link Action#getExternalRef()}
     * @return the found {@link Action}
     */
    Optional<Action> findByExternalRef(@Param("externalRef") String externalRef);

    /**
     * Switches the status of actions from one specific status into another for
     * given actions IDs, active flag and current status
     *
     * @param statusToSet
     *            the new status the actions should get
     * @param actionIds
     *            the IDs of the actions which are affected
     * @param active
     *            flag to indicate active/inactive actions
     * @param currentStatus
     *            the current status of the actions
     * @return the amount of updated actions
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.status = :statusToSet WHERE a.id IN :actionIds AND a.active = :active AND a.status = :currentStatus")
    int switchStatusForActionIdInAndIsActiveAndActionStatus(@Param("statusToSet") Action.Status statusToSet,
            @Param("actionIds") List<Long> actionIds, @Param("active") boolean active,
            @Param("currentStatus") Action.Status currentStatus);

    /**
     *
     * Retrieves all {@link Action}s which are active and referring to the given
     * target Ids and distribution set not requiring migration step.
     *
     * @param targetIds
     *            the IDs of targets for the actions
     * @param notStatus
     *            the status which the actions should not have
     * @return the found list of {@link Action}s
     */
    @EntityGraph(attributePaths = { "target" }, type = EntityGraphType.LOAD)
    @Query("SELECT a FROM JpaAction a WHERE a.active = true AND a.distributionSet.requiredMigrationStep = false AND a.target IN ?1 AND a.status != ?2")
    List<JpaAction> findByActiveAndTargetIdInAndActionStatusNotEqualToAndDistributionSetNotRequiredMigrationStep(
            Collection<Long> targetIds, Action.Status notStatus);

    /**
     *
     * Retrieves all {@link Action}s which are active and referring to the given
     * target Ids and distribution set not requiring migration step.
     *
     * @param targetIds
     *            the IDs of targets for the actions
     * @return the found list of {@link Action}s
     */
    @EntityGraph(attributePaths = { "target" }, type = EntityGraphType.LOAD)
    @Query("SELECT a FROM JpaAction a WHERE a.active = true AND a.distributionSet.requiredMigrationStep = false AND a.target IN ?1")
    List<JpaAction> findByActiveAndTargetIdInAndDistributionSetNotRequiredMigrationStep(Collection<Long> targetIds);

    /**
     * Counts all {@link Action}s referring to the given target.
     *
     * @param controllerId
     *            the target to count the {@link Action}s
     * @return the count of actions referring to the given target
     */
    Long countByTargetControllerId(String controllerId);

    /**
     * Counts all {@link Action}s referring to the given targetId.
     *
     * @param targetId
     *            the target to count the {@link Action}s
     * @return the count of actions referring to the given target
     */
    Long countByTargetId(Long targetId);

    /**
     * Counts all {@link Action}s referring to the given DistributionSet.
     *
     * @param distributionSet
     *            DistributionSet to count the {@link Action}s from
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetId(Long distributionSet);

    /**
     * Counts all active {@link Action}s referring to the given DistributionSet.
     *
     * @param distributionSet
     *            DistributionSet to count the {@link Action}s from
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetIdAndActiveIsTrue(Long distributionSet);

    /**
     * Counts all active {@link Action}s referring to the given DistributionSet
     * that are not in a given state.
     *
     * @param distributionSet
     *            DistributionSet to count the {@link Action}s from
     * @param status
     *            the state the actions should not have
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetIdAndActiveIsTrueAndStatusIsNot(Long distributionSet, Status status);

    /**
     * Counts all actions referring to a given rollout and rolloutgroup which
     * are currently not in the given status. An in-clause statement does not
     * work with the spring-data, so this is specific usecase regarding to the
     * rollout-management to find out actions which are not in specific states.
     *
     * @param rollout
     *            the rollout the actions are belong to
     * @param rolloutGroup
     *            the rolloutgroup the actions are belong to
     * @param statuses
     *            the list of statuses the action should not have
     * @return the count of actions referring the rollout and rolloutgroup and
     *         are not in given states
     */
    Long countByRolloutAndRolloutGroupAndStatusNotIn(JpaRollout rollout, JpaRolloutGroup rolloutGroup,
            List<Status> statuses);

    /**
     * Counts all actions referring to a given rollout and rolloutgroup.
     *
     * @param rollout
     *            the rollout the actions belong to
     * @param rolloutGroup
     *            the rolloutgroup the actions belong to
     * @return the count of actions referring to a rollout and rolloutgroup
     */
    Long countByRolloutAndRolloutGroup(JpaRollout rollout, JpaRolloutGroup rolloutGroup);

    /**
     * Counts all actions referring to a given rollout, rolloutgroup and status.
     *
     * @param rolloutId
     *            the ID of rollout the actions belong to
     * @param rolloutGroupId
     *            the ID rolloutgroup the actions belong to
     * @param status
     *            the status the actions should have
     * @return the count of actions referring to a rollout, rolloutgroup and are
     *         in a given status
     */
    Long countByRolloutIdAndRolloutGroupIdAndStatus(Long rolloutId, Long rolloutGroupId, Action.Status status);

    /**
     * Counts all actions referring to a given rollout and status.
     *
     * @param rolloutId
     *            the ID of the rollout the actions belong to
     * @param status
     *            the status the actions should have
     * @return the count of actions referring to a rollout and are in a given
     *         status
     */
    Long countByRolloutIdAndStatus(Long rolloutId, Action.Status status);

    /**
     * Returns {@code true} if actions for the given rollout exists, otherwise
     * {@code false}
     *
     * @param rolloutId
     *            the ID of the rollout the actions belong to
     * @return {@code true} if actions for the given rollout exists, otherwise
     *         {@code false}
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaAction a WHERE a.rollout.id=:rolloutId")
    boolean existsByRolloutId(@Param("rolloutId") Long rolloutId);

    /**
     * Returns {@code true} if actions for the given rollout exists, otherwise
     * {@code false}
     *
     * @param rolloutId
     *            the ID of the rollout the actions belong to
     * @param status
     *            the action is not to be in
     * @return {@code true} if actions for the given rollout exists, otherwise
     *         {@code false}
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaAction a WHERE a.rollout.id=:rolloutId AND a.status != :status")
    boolean existsByRolloutIdAndStatusNotIn(@Param("rolloutId") Long rolloutId, @Param("status") Status status);

    /**
     * Retrieving all actions referring to a given rollout with a specific action as
     * parent reference and a specific status.
     *
     * Finding all actions of a specific rolloutgroup parent relation.
     *
     * @param pageable
     *            page parameters
     * @param rollout
     *            the rollout the actions belong to
     * @param rolloutGroupParent
     *            the parent rolloutgroup the actions should reference
     * @param actionStatus
     *            the status the actions have
     * @return the actions referring a specific rollout and a specific parent
     *         rolloutgroup in a specific status
     */
    @EntityGraph(attributePaths = { "target", "target.autoConfirmationStatus", "rolloutGroup" }, type = EntityGraphType.LOAD)
    Page<Action> findByRolloutIdAndRolloutGroupParentIdAndStatus(Pageable pageable, Long rollout,
            Long rolloutGroupParent, Status actionStatus);

    /**
     * Retrieving all actions referring to the first group of a rollout.
     *
     * @param pageable
     *            page parameters
     * @param rollout
     *            the rollout the actions belong to
     * @param actionStatus
     *            the status the actions have
     * @return the actions referring a specific rollout and a specific parent
     *         rolloutgroup in a specific status
     */
    @EntityGraph(attributePaths = { "target", "target.autoConfirmationStatus",
            "rolloutGroup" }, type = EntityGraphType.LOAD)
    Page<Action> findByRolloutIdAndRolloutGroupParentIsNullAndStatus(Pageable pageable, Long rollout,
            Status actionStatus);

    /**
     * Retrieves all actions for a specific rollout and in a specific status.
     *
     * @param pageable
     *            page parameters
     * @param rolloutId
     *            the rollout the actions belong to
     * @param actionStatus
     *            the status of the actions
     * @return the actions referring a specific rollout an in a specific status
     */
    Page<JpaAction> findByRolloutIdAndStatus(Pageable pageable, Long rolloutId, Status actionStatus);

    /**
     * Get list of objects which has details of status and count of targets in
     * each status in specified rollout.
     *
     * @param rolloutId
     *            id of {@link Rollout}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus( a.rollout.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rollout.id IN ?1 GROUP BY a.rollout.id,a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutId(List<Long> rolloutId);

    /**
     * Get list of objects which has details of status and count of targets in
     * each status in specified rollout.
     *
     * @param rolloutId
     *            id of {@link Rollout}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus( a.rollout.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rollout.id = ?1 GROUP BY a.rollout.id,a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutId(Long rolloutId);

    /**
     * Get list of objects which has details of status and count of targets in
     * each status in specified rollout group.
     *
     * @param rolloutGroupId
     *            id of {@link RolloutGroup}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus(a.rolloutGroup.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rolloutGroup.id = ?1 GROUP BY a.rolloutGroup.id, a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutGroupId(Long rolloutGroupId);

    /**
     * Get list of objects which has details of status and count of targets in
     * each status in specified rollout group.
     *
     * @param rolloutGroupId
     *            list of id of {@link RolloutGroup}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus(a.rolloutGroup.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rolloutGroup.id IN ?1 GROUP BY a.rolloutGroup.id, a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutGroupId(List<Long> rolloutGroupId);

    /**
     * Deletes all actions with the given IDs.
     *
     * @param actionIDs
     *            the IDs of the actions to be deleted.
     */
    @Modifying
    @Transactional
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("DELETE FROM JpaAction a WHERE a.id IN ?1")
    void deleteByIdIn(Collection<Long> actionIDs);

    /**
     * Updates the externalRef of an action by its actionId.
     *
     * @param actionId
     *            for which the externalRef is being updated.
     * @param externalRef
     *            value of the external reference for the given action id.
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.externalRef = :externalRef WHERE a.id = :actionId")
    void updateExternalRef(@Param("actionId") Long actionId, @Param("externalRef") String externalRef);
}
