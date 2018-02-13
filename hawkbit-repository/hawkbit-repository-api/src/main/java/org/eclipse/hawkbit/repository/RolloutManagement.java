/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.List;
import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * RolloutManagement to control rollouts e.g. like creating, starting, resuming
 * and pausing rollouts. This service secures all the functionality based on the
 * {@link PreAuthorize} annotation on methods.
 */
public interface RolloutManagement {

    /**
     * Process rollout based on its current {@link Rollout#getStatus()}.
     * 
     * For {@link RolloutStatus#CREATING} that means creating the
     * {@link RolloutGroup}s with {@link Target}s and when finished switch to
     * {@link RolloutStatus#READY}.
     * 
     * For {@link RolloutStatus#READY} that means switching to
     * {@link RolloutStatus#STARTING} if the {@link Rollout#getStartAt()} is set
     * and time of calling this method is beyond this point in time. This auto
     * start mechanism is optional. Call {@link #start(Long)} otherwise.
     * 
     * For {@link RolloutStatus#STARTING} that means starting the first
     * {@link RolloutGroup}s in line and when finished switch to
     * {@link RolloutStatus#RUNNING}.
     * 
     * For {@link RolloutStatus#RUNNING} that means checking to activate further
     * groups based on the defined thresholds. Switched to
     * {@link RolloutStatus#FINISHED} is all groups are finished.
     * 
     * For {@link RolloutStatus#DELETING} that means either soft delete in case
     * rollout was already {@link RolloutStatus#RUNNING} which results in status
     * change {@link RolloutStatus#DELETED} or hard delete from the persistence
     * otherwise.
     * 
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void handleRollouts();

    /**
     * Counts all {@link Rollout}s in the repository that are not marked as
     * deleted.
     *
     * @return number of roll outs
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    long count();

    /**
     * Count rollouts by given text in name or description.
     *
     * @param searchText
     *            name or description
     * @return total count rollouts for specified filter text.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    long countByFilters(@NotEmpty String searchText);

    /**
     * Persists a new rollout entity. The filter within the
     * {@link Rollout#getTargetFilterQuery()} is used to retrieve the targets
     * which are effected by this rollout to create. The amount of groups will
     * be defined as equally sized.
     *
     * The rollout is not started. Only the preparation of the rollout is done,
     * creating and persisting all the necessary groups. The Rollout and the
     * groups are persisted in {@link RolloutStatus#CREATING} and
     * {@link RolloutGroupStatus#CREATING}.
     *
     * The RolloutScheduler will start to assign targets to the groups. Once all
     * targets have been assigned to the groups, the rollout status is changed
     * to {@link RolloutStatus#READY} so it can be started with
     * {@link #start(Rollout)}.
     *
     * @param create
     *            the rollout entity to create
     * @param amountGroup
     *            the amount of groups to split the rollout into
     * @param conditions
     *            the rolloutgroup conditions and actions which should be
     *            applied for each {@link RolloutGroup}
     * @return the persisted rollout.
     *
     * @throws EntityNotFoundException
     *             if given {@link DistributionSet} does not exist
     * @throws ConstraintViolationException
     *             if rollout or group parameters are invalid.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE)
    Rollout create(@NotNull RolloutCreate create, int amountGroup, @NotNull RolloutGroupConditions conditions);

    /**
     * Persists a new rollout entity. The filter within the
     * {@link Rollout#getTargetFilterQuery()} is used to filter the targets
     * which are affected by this rollout. The given groups will be used to
     * create the groups.
     *
     * The rollout is not started. Only the preparation of the rollout is done,
     * creating and persisting all the necessary groups. The Rollout and the
     * groups are persisted in {@link RolloutStatus#CREATING} and
     * {@link RolloutGroupStatus#CREATING}.
     *
     * The RolloutScheduler will start to assign targets to the groups. Once all
     * targets have been assigned to the groups, the rollout status is changed
     * to {@link RolloutStatus#READY} so it can be started with
     * {@link #start(Rollout)}.
     *
     * @param rollout
     *            the rollout entity to create
     * @param groups
     *            optional definition of groups
     * @param conditions
     *            the rollout group conditions and actions which should be
     *            applied for each {@link RolloutGroup} if not defined by the
     *            RolloutGroup itself
     * @return the persisted rollout.
     *
     * @throws EntityNotFoundException
     *             if given {@link DistributionSet} does not exist
     * @throws ConstraintViolationException
     *             if rollout or group parameters are invalid
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE)
    Rollout create(@NotNull @Valid RolloutCreate rollout, @NotNull @Valid List<RolloutGroupCreate> groups,
            RolloutGroupConditions conditions);

    /**
     * Calculates how many targets are addressed by each rollout group and
     * returns the validation information.
     *
     * @param groups
     *            a list of rollout groups
     * @param targetFilter
     *            the rollout
     * @param createdAt
     *            timestamp when the rollout was created
     * @return the validation information
     * @throws RolloutIllegalStateException
     *             thrown when no targets are targeted by the rollout
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link RolloutGroupCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    ListenableFuture<RolloutGroupsValidation> validateTargetsInGroups(@Valid List<RolloutGroupCreate> groups,
            String targetFilter, Long createdAt);

    /**
     * Retrieves all rollouts.
     *
     * @param pageable
     *            the page request to sort and limit the result
     * @param deleted
     *            flag if deleted rollouts should be included
     * @return a page of found rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Rollout> findAll(@NotNull Pageable pageable, boolean deleted);

    /**
     * Get count of targets in different status in rollout.
     *
     * @param pageable
     *            the page request to sort and limit the result
     * @param deleted
     *            flag if deleted rollouts should be included
     * @return a list of rollouts with details of targets count for different
     *         statuses
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Rollout> findAllWithDetailedStatus(@NotNull Pageable pageable, boolean deleted);

    /**
     * Retrieves all rollouts found by the given specification.
     * 
     * @param pageable
     *            the page request to sort and limit the result
     * @param rsqlParam
     *            the specification to filter rollouts
     * @param deleted
     *            flag if deleted rollouts should be included
     *
     * @return a page of found rollouts
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Rollout> findByRsql(@NotNull Pageable pageable, @NotNull String rsqlParam, boolean deleted);

    /**
     * Finds rollouts by given text in name or description.
     *
     * @param pageable
     *            the page request to sort and limit the result
     * @param searchText
     *            search text which matches name or description of rollout
     * @param deleted
     *            flag if deleted rollouts should be included
     * @return the founded rollout or {@code null} if rollout with given ID does
     *         not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Slice<Rollout> findByFiltersWithDetailedStatus(@NotNull Pageable pageable, @NotEmpty String searchText,
            boolean deleted);

    /**
     * Retrieves a specific rollout by its ID.
     *
     * @param rolloutId
     *            the ID of the rollout to retrieve
     * @return the founded rollout or {@code null} if rollout with given ID does
     *         not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Optional<Rollout> get(long rolloutId);

    /**
     * Retrieves a specific rollout by its name.
     *
     * @param rolloutName
     *            the name of the rollout to retrieve
     * @return the founded rollout or {@code null} if rollout with given name
     *         does not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Optional<Rollout> getByName(@NotEmpty String rolloutName);

    /**
     * Get count of targets in different status in rollout.
     *
     * @param rolloutId
     *            rollout id
     * @param deleted
     *            flag if deleted rollouts should be included
     * @return rollout details of targets count for different statuses
     * 
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Optional<Rollout> getWithDetailedStatus(long rolloutId);

    /**
     * Checks if rollout with given ID exists.
     *
     * @param rolloutId
     *            rollout id
     * 
     * @return <code>true</code> if rollout exists
     */
    boolean exists(long rolloutId);

    /**
     * Pauses a rollout which is currently running. The Rollout switches
     * {@link RolloutStatus#PAUSED}. {@link RolloutGroup}s which are currently
     * running will be untouched. {@link RolloutGroup}s which are
     * {@link RolloutGroupStatus#SCHEDULED} will not be started and keep in
     * {@link RolloutGroupStatus#SCHEDULED} state until the rollout is
     * {@link RolloutManagement#resumeRollout(Rollout)}.
     *
     * Switching the rollout status to {@link RolloutStatus#PAUSED} is
     * sufficient due the {@link #checkRunningRollouts(long)} will not check
     * this rollout anymore.
     *
     * @param rolloutId
     *            the rollout to be paused.
     *
     * @throws EntityNotFoundException
     *             if rollout or group with given ID does not exist
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#RUNNING}.
     *             Only running rollouts can be paused.
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void pauseRollout(long rolloutId);

    /**
     * Resumes a paused rollout. The rollout switches back to
     * {@link RolloutStatus#RUNNING} state which is then picked up again by the
     * {@link #checkRunningRollouts(long)}.
     *
     * @param rolloutId
     *            the rollout to be resumed
     * 
     * @throws EntityNotFoundException
     *             if rollout with given ID does not exist
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#PAUSED}. Only
     *             paused rollouts can be resumed.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void resumeRollout(long rolloutId);

    /**
     * Starts a rollout which has been created. The rollout must be in
     * {@link RolloutStatus#READY} state. The Rollout will be set into the
     * {@link RolloutStatus#STARTING} state. The RolloutScheduler will ensure
     * all actions are created and the first group is started. The rollout
     * itself will be then also in {@link RolloutStatus#RUNNING}.
     *
     * @param rolloutId
     *            the rollout to be started
     *
     * @return started rollout
     * 
     * @throws EntityNotFoundException
     *             if rollout with given ID does not exist
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#READY}. Only
     *             ready rollouts can be started.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    Rollout start(long rolloutId);

    /**
     * Update rollout details.
     *
     * @param update
     *            rollout to be updated
     *
     * @return Rollout updated rollout
     * 
     * @throws EntityNotFoundException
     *             if rollout or DS with given IDs do not exist
     * @throws EntityReadOnlyException
     *             if rollout is in soft deleted state, i.e. only kept as
     *             reference
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE)
    Rollout update(@NotNull @Valid RolloutUpdate update);

    /**
     * Deletes a rollout. A rollout might be deleted asynchronously by
     * indicating the rollout by {@link RolloutStatus#DELETING}
     * 
     * 
     * @param rolloutId
     *            the ID of the rollout to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void delete(long rolloutId);

}
