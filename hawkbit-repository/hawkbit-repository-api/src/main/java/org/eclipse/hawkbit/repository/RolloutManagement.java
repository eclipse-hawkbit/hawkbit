/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.List;
import java.util.Optional;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * RolloutManagement to control rollouts e.g. like creating, starting, resuming and pausing rollouts. This service secures all the
 * functionality based on the {@link PreAuthorize} annotation on methods.
 */
public interface RolloutManagement extends PermissionSupport {

    String HAS_ROLLOUT_APPROVE = "hasPermission(#root, 'APPROVE_${permissionGroup}')";
    String HAS_ROLLOUT_HANDLE = "hasPermission(#root, 'HANDLE_${permissionGroup}')";

    @Override
    default String permissionGroup() {
        return SpPermission.ROLLOUT;
    }

    /**
     * Counts all {@link Rollout}s in the repository that are not marked as deleted.
     *
     * @return number of rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    long count();

    /**
     * Counts all {@link Rollout}s for a specific {@link DistributionSet} that
     * are stoppable
     * <p/>
     * No access control applied
     *
     * @param setId the distribution set
     * @return the count
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    long countByDistributionSetIdAndRolloutIsStoppable(long setId);

    /**
     * Persists a new rollout entity. The filter within the {@link Rollout#getTargetFilterQuery()} is used to retrieve the targets which
     * are effected by this rollout to create. The amount of groups will be defined as equally sized.
     * <p/>
     * The rollout is not started. Only the preparation of the rollout is done, creating and persisting all the necessary groups. The Rollout
     * and the groups are persisted in {@link RolloutStatus#CREATING} and {@link RolloutGroupStatus#CREATING}.
     * <p/>
     * The RolloutScheduler will start to assign targets to the groups. Once all targets have been assigned to the groups, the rollout status
     * is changed to {@link RolloutStatus#READY} so it can be started with .
     *
     * @param create the rollout entity to create
     * @param amountGroup the amount of groups to split the rollout into
     * @param confirmationRequired if a confirmation is required by the device group(s) of the rollout
     * @param conditions the rolloutgroup conditions and actions which should be applied for each {@link RolloutGroup}
     * @param dynamicRolloutGroupTemplate the template for dynamic rollout groups
     * @return the persisted rollout.
     * @throws EntityNotFoundException if given {@link DistributionSet} does not exist
     * @throws ConstraintViolationException if rollout or group parameters are invalid.
     * @throws AssignmentQuotaExceededException if the maximum number of allowed targets per rollout group is
     *         exceeded.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_CREATE_REPOSITORY)
    Rollout create(
            @NotNull @Valid Create create, int amountGroup, boolean confirmationRequired,
            @NotNull RolloutGroupConditions conditions, DynamicRolloutGroupTemplate dynamicRolloutGroupTemplate);

    /**
     * Persists a new rollout entity. The filter within the {@link Rollout#getTargetFilterQuery()} is used to retrieve the targets which
     * are effected by this rollout to create. The amount of groups will be defined as equally sized.
     * <p/>
     * The rollout is not started. Only the preparation of the rollout is done, creating and persisting all the necessary groups. The Rollout
     * and the groups are persisted in {@link RolloutStatus#CREATING} and {@link RolloutGroupStatus#CREATING}.
     * <p/>
     * The RolloutScheduler will start to assign targets to the groups. Once all targets have been assigned to the groups, the rollout status is
     * changed to {@link RolloutStatus#READY} so it can be started with .
     *
     * @param create the rollout entity to create
     * @param amountGroup the amount of groups to split the rollout into
     * @param confirmationRequired if a confirmation is required by the device group(s) of the rollout
     * @param conditions the rolloutgroup conditions and actions which should be applied for each {@link RolloutGroup}
     * @return the persisted rollout.
     * @throws EntityNotFoundException if given {@link DistributionSet} does not exist
     * @throws ConstraintViolationException if rollout or group parameters are invalid.
     * @throws AssignmentQuotaExceededException if the maximum number of allowed targets per rollout group is exceeded.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_CREATE_REPOSITORY)
    Rollout create(@NotNull @Valid Create create, int amountGroup, boolean confirmationRequired, @NotNull RolloutGroupConditions conditions);

    /**
     * Persists a new rollout entity. The filter within the {@link Rollout#getTargetFilterQuery()} is used to filter the targets which
     * are affected by this rollout. The given groups will be used to create the groups.
     * <p/>
     * The rollout is not started. Only the preparation of the rollout is done, creating and persisting all the necessary groups. The Rollout
     * and the groups are persisted in {@link RolloutStatus#CREATING} and {@link RolloutGroupStatus#CREATING}.
     * <p/>
     * The RolloutScheduler will start to assign targets to the groups. Once all targets have been assigned to the groups, the rollout status is
     * changed to {@link RolloutStatus#READY} so it can be started with {@link #start(long)}.
     *
     * @param rollout the rollout entity to create
     * @param groups optional definition of groups
     * @param conditions the rollout group conditions and actions which should be applied for each {@link RolloutGroup} if not defined by the
     *         RolloutGroup itself
     * @return the persisted rollout.
     * @throws EntityNotFoundException if given {@link DistributionSet} does not exist
     * @throws ConstraintViolationException if rollout or group parameters are invalid
     * @throws AssignmentQuotaExceededException if the maximum number of allowed targets per rollout group is exceeded.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_CREATE_REPOSITORY)
    Rollout create(@Valid @NotNull Create rollout, @NotNull @Valid List<GroupCreate> groups, RolloutGroupConditions conditions);

    /**
     * Retrieves all rollouts.
     *
     * @param deleted flag if deleted rollouts should be included
     * @param pageable the page request to sort and limit the result
     * @return a page of found rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<Rollout> findAll(boolean deleted, @NotNull Pageable pageable);

    /**
     * Get count of targets in different status in rollout.
     *
     * @param deleted flag if deleted rollouts should be included
     * @param pageable the page request to sort and limit the result
     * @return a list of rollouts with details of targets count for different statuses
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<Rollout> findAllWithDetailedStatus(boolean deleted, @NotNull Pageable pageable);

    /**
     * Retrieves all rollouts found by the given specification.
     *
     * @param rsql the specification to filter rollouts
     * @param deleted flag if deleted rollouts should be included
     * @param pageable the page request to sort and limit the result
     * @return a page of found rollouts
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<Rollout> findByRsql(@NotNull String rsql, boolean deleted, @NotNull Pageable pageable);

    /**
     * Finds rollouts by given text in name or description.
     *
     * @param rsql search text which matches name or description of rollout
     * @param deleted flag if deleted rollouts should be included
     * @param pageable the page request to sort and limit the result
     * @return the founded rollout or {@code null} if rollout with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<Rollout> findByRsqlWithDetailedStatus(@NotEmpty String rsql, boolean deleted, @NotNull Pageable pageable);

    /**
     * Find rollouts which are still active and needs to be handled.
     *
     * @return a list of active rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    List<Long> findActiveRollouts();

    /**
     * Retrieves a specific rollout by its ID. Throws exception if not found.
     *
     * @param rolloutId the ID of the rollout to retrieve
     * @return the found rollout or empty if rollout with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Rollout get(long rolloutId);

    /**
     * Retrieves a specific rollout by its ID.
     *
     * @param rolloutId the ID of the rollout to retrieve
     * @return the found rollout or empty if rollout with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<Rollout> find(long rolloutId);

    /**
     * Get count of targets in different status in rollout.
     *
     * @param rolloutId rollout id
     * @return rollout details of targets count for different statuses
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Rollout getWithDetailedStatus(long rolloutId);

    /**
     * Checks if rollout with given ID exists.
     *
     * @param rolloutId rollout id
     * @return <code>true</code> if rollout exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    boolean exists(long rolloutId);

    /**
     * Pauses a rollout which is currently running. The Rollout switches
     * {@link RolloutStatus#PAUSED}. {@link RolloutGroup}s which are currently
     * running will be untouched. {@link RolloutGroup}s which are
     * {@link RolloutGroupStatus#SCHEDULED} will not be started and keep in
     * {@link RolloutGroupStatus#SCHEDULED} state until the rollout is
     * {@link RolloutManagement#resumeRollout(long)}.
     * <p/>
     * Switching the rollout status to {@link RolloutStatus#PAUSED} is sufficient.
     *
     * @param rolloutId the rollout to be paused.
     * @throws EntityNotFoundException if rollout or group with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#RUNNING}. Only running rollouts can be paused.
     */
    @PreAuthorize(HAS_ROLLOUT_HANDLE)
    void pauseRollout(long rolloutId);

    /**
     * Resumes a paused rollout. The rollout switches back to {@link RolloutStatus#RUNNING} state which is then picked up again executor.
     *
     * @param rolloutId the rollout to be resumed
     * @throws EntityNotFoundException if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#PAUSED}. Only paused rollouts can be resumed.
     */
    @PreAuthorize(HAS_ROLLOUT_HANDLE)
    void resumeRollout(long rolloutId);

    /**
     * Approves or denies a created rollout being in state {@link RolloutStatus#WAITING_FOR_APPROVAL}. If the rollout is approved,
     * it switches state to {@link RolloutStatus#READY}, otherwise it switches to state {@link RolloutStatus#APPROVAL_DENIED}
     *
     * @param rolloutId the rollout to be approved or denied.
     * @param decision decision whether a rollout is approved or denied.
     * @return approved or denied rollout
     * @throws EntityNotFoundException if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#WAITING_FOR_APPROVAL}. Only rollouts
     *         waiting for approval can be acted upon.
     */
    @PreAuthorize(HAS_ROLLOUT_APPROVE)
    Rollout approveOrDeny(long rolloutId, Rollout.ApprovalDecision decision);

    /**
     * Approves or denies a created rollout being in state
     * {@link RolloutStatus#WAITING_FOR_APPROVAL}. If the rollout is approved,
     * it switches state to {@link RolloutStatus#READY}, otherwise it switches
     * to state {@link RolloutStatus#APPROVAL_DENIED}
     *
     * @param rolloutId the rollout to be approved or denied.
     * @param decision decision whether a rollout is approved or denied.
     * @param remark user remark on approve / deny decision
     * @return approved or denied rollout
     * @throws EntityNotFoundException if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in
     *         {@link RolloutStatus#WAITING_FOR_APPROVAL}. Only rollouts
     *         waiting for approveOrDeny can be acted upon.
     */
    @PreAuthorize(HAS_ROLLOUT_APPROVE)
    Rollout approveOrDeny(long rolloutId, Rollout.ApprovalDecision decision, String remark);

    /**
     * Starts a rollout which has been created. The rollout must be in
     * {@link RolloutStatus#READY} state. The Rollout will be set into the
     * {@link RolloutStatus#STARTING} state. The RolloutScheduler will ensure
     * all actions are created and the first group is started. The rollout
     * itself will be then also in {@link RolloutStatus#RUNNING}.
     *
     * @param rolloutId the rollout to be started
     * @return started rollout
     * @throws EntityNotFoundException if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#READY}. Only
     *         ready rollouts can be started.
     */
    @PreAuthorize(HAS_ROLLOUT_HANDLE)
    Rollout start(long rolloutId);

    /**
     * Update rollout details.
     *
     * @param update rollout to be updated
     * @return Rollout updated rollout
     * @throws EntityNotFoundException if rollout or DS with given IDs do not exist
     * @throws EntityReadOnlyException if rollout is in soft deleted state, i.e. only kept as
     *         reference
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    Rollout update(@NotNull @Valid Update update);

    /**
     * Stop a rollout
     *
     * @param rolloutId of the rollout to be stopped
     * @return stopped rollout
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    Rollout stop(long rolloutId);

    /**
     * Deletes a rollout. A rollout might be deleted asynchronously by
     * indicating the rollout by {@link RolloutStatus#DELETING}
     *
     * @param rolloutId the ID of the rollout to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_DELETE_REPOSITORY)
    void delete(long rolloutId);

    /**
     * Cancels all rollouts that refer to the given {@link DistributionSet}.
     * This is called when a distribution set is invalidated and the cancel
     * rollouts option is activated.
     *
     * @param set the {@link DistributionSet} for that the rollouts should be
     *         canceled
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void cancelRolloutsForDistributionSet(DistributionSet set, ActionCancellationType cancelationType);

    /**
     * Triggers next group of a rollout for processing even success threshold
     * isn't met yet. Current running groups will not change their status.
     *
     * @param rolloutId the rollout to be paused.
     * @throws EntityNotFoundException if rollout or group with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#RUNNING}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void triggerNextGroup(long rolloutId);

    /**
     * Cancels all actions that refer to a given rollout.
     *
     * @param cancelationType - type of cancellation - FORCE or SOFT (NONE is ignored)
     * @param rollout - the rollout which actions are about to be cancelled
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void cancelActiveActionsForRollouts(final Rollout rollout, final ActionCancellationType cancelationType);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {

        @ValidString
        @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE)
        @NotNull
        private String targetFilterQuery;
        @NotNull
        private DistributionSet distributionSet;
        private boolean dynamic;
        @Builder.Default
        @NotNull
        private Action.ActionType actionType = Action.ActionType.FORCED;
        private Long forcedTime;
        private Long startAt;
        @Min(Action.WEIGHT_MIN)
        @Max(Action.WEIGHT_MAX)
        private Integer weight;
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Update extends UpdateCreate implements Identifiable<Long> {

        @NotNull
        private Long id;
    }

    @SuperBuilder
    @Getter
    class UpdateCreate {

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull(groups = Create.class)
        private String name;
        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;
    }

    @Getter
    class GroupCreate {

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull
        private final String name;
        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private final String description;
        @ValidString
        @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE)
        private final String targetFilterQuery;
        private final Float targetPercentage;
        private final boolean confirmationRequired;

        // Conditions and actions for the rollout group
        private final RolloutGroup.RolloutGroupSuccessCondition successCondition;
        private final String successConditionExp;
        private final RolloutGroup.RolloutGroupSuccessAction successAction;
        private final String successActionExp;
        private final RolloutGroup.RolloutGroupErrorCondition errorCondition;
        private final String errorConditionExp;
        private final RolloutGroup.RolloutGroupErrorAction errorAction;
        private final String errorActionExp;

        private GroupCreate(
                final String name, final String description, final String targetFilterQuery, final Float targetPercentage,
                final RolloutGroupConditions conditions, final boolean confirmationRequired) {
            this.name = name;
            this.description = description;
            this.targetFilterQuery = targetFilterQuery;
            this.targetPercentage = targetPercentage;

            this.successCondition = conditions.getSuccessCondition();
            this.successConditionExp = conditions.getSuccessConditionExp();
            this.successAction = conditions.getSuccessAction();
            this.successActionExp = conditions.getSuccessActionExp();
            this.errorCondition = conditions.getErrorCondition();
            this.errorConditionExp = conditions.getErrorConditionExp();
            this.errorAction = conditions.getErrorAction();
            this.errorActionExp = conditions.getErrorActionExp();

            this.confirmationRequired = confirmationRequired;
        }

        public static GroupCreateBuilder builder() {
            return new GroupCreateBuilder();
        }

        @Setter
        @Accessors(fluent = true)
        public static class GroupCreateBuilder {

            private String name;
            private String description;
            private String targetFilterQuery;
            private Float targetPercentage;
            private RolloutGroupConditions conditions;
            private boolean confirmationRequired;

            public GroupCreate build() {
                return new GroupCreate(
                        name, description, targetFilterQuery, targetPercentage,
                        conditions == null ? new RolloutGroupConditionBuilder().withDefaults().build() : conditions,
                        confirmationRequired);
            }
        }
    }

    /**
     * Builder to create a new dynamic rollout group secret
     */
    @Data
    @Builder
    class DynamicRolloutGroupTemplate {

        /**
         * The name suffix, by default "" is used.
         */
        @NotNull
        private String nameSuffix = "";

        /**
         * The count of matching Targets that should be assigned to this Group
         */
        private long targetCount;

        /**
         * The group conditions
         */
        private RolloutGroupConditions conditions;

        /**
         * If confirmation is required for this rollout group (considered with confirmation flow active)
         */
        private boolean confirmationRequired;
    }
}