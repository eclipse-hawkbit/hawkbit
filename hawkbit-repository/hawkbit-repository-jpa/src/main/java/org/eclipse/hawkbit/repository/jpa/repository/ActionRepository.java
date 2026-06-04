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

import java.util.List;
import java.util.Optional;

import jakarta.persistence.criteria.CriteriaBuilder;

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
     * Retrieving all actions referring to a given rollout with a specific action as parent reference and a specific status.
     * <p/>
     * Finding all actions of a specific rollout group parent relation.
     * <p/>
     * No access control applied
     *
     * @param pageable page parameters
     * @param rollout the rollout the actions belong to
     * @param rolloutGroupParent the parent rollout group the actions should reference
     * @param actionStatus the status the actions have
     * @return the actions referring a specific rollout and a specific parent rolloutgroup in a specific status
     */
    @EntityGraph(attributePaths = { "target", "target.autoConfirmationStatus", "rolloutGroup" }, type = EntityGraphType.LOAD)
    Page<Action> findByRolloutIdAndRolloutGroupParentIdAndStatus(Pageable pageable, Long rollout, Long rolloutGroupParent, Status actionStatus);

    /**
     * Retrieving all actions referring to the first group of a rollout.
     * <p/>
     * No access control applied
     *
     * @param pageable page parameters
     * @param rollout the rollout the actions belong to
     * @param actionStatus the status the actions have
     * @return the actions referring a specific rollout and a specific parent rolloutgroup in a specific status
     */
    @EntityGraph(attributePaths = { "target", "target.autoConfirmationStatus", "rolloutGroup" }, type = EntityGraphType.LOAD)
    Page<Action> findByRolloutIdAndRolloutGroupParentIsNullAndStatus(Pageable pageable, Long rollout, Status actionStatus);

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