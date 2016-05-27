/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * RolloutManagement to control rollouts e.g. like creating, starting, resuming
 * and pausing rollouts. This service secures all the functionality based on the
 * {@link PreAuthorize} annotation on methods.
 */
public interface RolloutManagement {

    /**
     * Checking running rollouts. Rollouts which are checked updating the
     * {@link Rollout#setLastCheck(long)} to indicate that the current instance
     * is handling the specific rollout. This code should run as system-code.
     *
     * <pre>
     * {@code
     *  SystemSecurityContext.runAsSystem(new Callable<Void>() {
     *     public Void call() throws Exception {
     *        //run system-code
     *     }
     * });
     *  }
     * </pre>
     *
     * This method is attend to be called by a scheduler.
     * {@link RolloutScheduler}. And must be running in an transaction so it's
     * splitted from the scheduler.
     *
     * Rollouts which are currently running are investigated, by means the
     * error- and finish condition of running groups in this rollout are
     * evaluated.
     *
     * @param delayBetweenChecks
     *            the time in milliseconds of the delay between the further and
     *            this check. This check is only applied if the last check is
     *            less than (lastcheck-delay).
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    void checkRunningRollouts(long delayBetweenChecks);

    /**
     * Counts all {@link Rollout}s in the repository.
     *
     * @return number of roll outs
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Long countRolloutsAll();

    /**
     * Count rollouts by given text in name or description.
     *
     * @param searchText
     *            name or description
     * @return total count rollouts for specified filter text.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Long countRolloutsAllByFilters(@NotEmpty String searchText);

    /**
     * Persists a new rollout entity. The filter within the
     * {@link Rollout#getTargetFilterQuery()} is used to retrieve the targets
     * which are effected by this rollout to create. The targets will then be
     * split up into groups. The size of the groups can be defined in the
     * {@code groupSize} parameter.
     *
     * The rollout is not started. Only the preparation of the rollout is done,
     * persisting and creating all the necessary groups. The Rollout and the
     * groups are persisted in {@link RolloutStatus#READY} and
     * {@link RolloutGroupStatus#READY} so they can be started
     * {@link #startRollout(Rollout)}.
     *
     * @param rollout
     *            the rollout entity to create
     * @param amountGroup
     *            the amount of groups to split the rollout into
     * @param conditions
     *            the rolloutgroup conditions and actions which should be
     *            applied for each {@link RolloutGroup}
     * @return the persisted rollout.
     *
     * @throws IllegalArgumentException
     *             in case the given groupSize is zero or lower.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE)
    Rollout createRollout(@NotNull Rollout rollout, int amountGroup, @NotNull RolloutGroupConditions conditions);

    /**
     * Persists a new rollout entity. The filter within the
     * {@link Rollout#getTargetFilterQuery()} is used to retrieve the targets
     * which are effected by this rollout to create. The creation of the rollout
     * will be done synchronously and will be returned. The targets will then be
     * split up into groups. The size of the groups can be defined in the
     * {@code groupSize} parameter.
     *
     * The creation of the rollout groups is executed asynchronously due it
     * might take some time to split up the targets into groups. The creation of
     * the {@link RolloutGroup} is published as event
     * {@link RolloutGroupCreatedEvent}.
     *
     * The rollout is in status {@link RolloutStatus#CREATING} until all rollout
     * groups has been created and the targets are split up, then the rollout
     * will change the status to {@link RolloutStatus#READY}.
     *
     * The rollout is not started. Only the preparation of the rollout is done,
     * persisting and creating all the necessary groups. The Rollout and the
     * groups are persisted in {@link RolloutStatus#READY} and
     * {@link RolloutGroupStatus#READY} so they can be started
     * {@link #startRollout(Rollout)}.
     *
     * @param rollout
     *            the rollout to be created
     * @param amountGroup
     *            the number of groups should be created for the rollout and
     *            split up the targets
     * @param conditions
     *            the rolloutgroup conditions and actions which should be
     *            applied for each {@link RolloutGroup}
     * @return the created rollout entity in state
     *         {@link RolloutStatus#CREATING}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE)
    Rollout createRolloutAsync(@NotNull Rollout rollout, int amountGroup, @NotNull RolloutGroupConditions conditions);

    /**
     * Retrieves all rollouts.
     *
     * @param pageable
     *            the page request to sort and limit the result
     * @return a page of found rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Rollout> findAll(@NotNull Pageable pageable);

    /**
     * Get count of targets in different status in rollout.
     *
     * @param pageable
     *            the page request to sort and limit the result
     * @return a list of rollouts with details of targets count for different
     *         statuses
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Rollout> findAllRolloutsWithDetailedStatus(@NotNull Pageable pageable);

    /**
     * Retrieves all rollouts found by the given specification.
     *
     * @param rsqlParam
     *            the specification to filter rollouts
     * @param pageable
     *            the page request to sort and limit the result
     * @return a page of found rollouts
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Rollout> findAllWithDetailedStatusByPredicate(@NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * Finds rollouts by given text in name or description.
     *
     * @param pageable
     *            the page request to sort and limit the result
     * @param searchText
     *            search text which matches name or description of rollout
     * @return the founded rollout or {@code null} if rollout with given ID does
     *         not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Slice<Rollout> findRolloutByFilters(@NotNull Pageable pageable, @NotEmpty String searchText);

    /**
     * Retrieves a specific rollout by its ID.
     *
     * @param rolloutId
     *            the ID of the rollout to retrieve
     * @return the founded rollout or {@code null} if rollout with given ID does
     *         not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Rollout findRolloutById(@NotNull Long rolloutId);

    /**
     * Retrieves a specific rollout by its name.
     *
     * @param rolloutName
     *            the name of the rollout to retrieve
     * @return the founded rollout or {@code null} if rollout with given name
     *         does not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Rollout findRolloutByName(@NotNull String rolloutName);

    /**
     * Get count of targets in different status in rollout.
     *
     * @param rolloutId
     *            rollout id
     * @return rollout details of targets count for different statuses
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Rollout findRolloutWithDetailedStatus(@NotNull Long rolloutId);

    /***
     * Get finished percentage details for a specified group which is in running
     * state.
     *
     * @param rolloutId
     *            the ID of the {@link Rollout}
     * @param rolloutGroup
     *            the ID of the {@link RolloutGroup}
     * @return percentage finished
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    float getFinishedPercentForRunningGroup(@NotNull Long rolloutId, @NotNull RolloutGroup rolloutGroup);

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
     * @param rollout
     *            the rollout to be paused.
     *
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#RUNNING}.
     *             Only running rollouts can be paused.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    void pauseRollout(@NotNull Rollout rollout);

    /**
     * Resumes a paused rollout. The rollout switches back to
     * {@link RolloutStatus#RUNNING} state which is then picked up again by the
     * {@link #checkRunningRollouts(long)}.
     *
     * @param rollout
     *            the rollout to be resumed
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#PAUSED}. Only
     *             paused rollouts can be resumed.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    void resumeRollout(@NotNull Rollout rollout);

    /**
     * Starts a rollout which has been created. The rollout must be in
     * {@link RolloutStatus#READY} state. The according actions will be created
     * for each affected target in the rollout. The actions of the first group
     * will be started immediately {@link RolloutGroupStatus#RUNNING} as the
     * other groups will be {@link RolloutGroupStatus#SCHEDULED} state.
     *
     * The rollout itself will be then also in {@link RolloutStatus#RUNNING}.
     *
     * @param rollout
     *            the rollout to be started
     *
     * @return started rollout
     *
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#READY}. Only
     *             ready rollouts can be started.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    Rollout startRollout(@NotNull Rollout rollout);

    /**
     * Starts a rollout asynchronously which has been created. The rollout must
     * be in {@link RolloutStatus#READY} state. The according actions will be
     * created asynchronously for each affected target in the rollout. The
     * actions of the first group will be started immediately
     * {@link RolloutGroupStatus#RUNNING} as the other groups will be
     * {@link RolloutGroupStatus#SCHEDULED} state.
     *
     * The rollout itself will be then also in {@link RolloutStatus#RUNNING}.
     *
     * @param rollout
     *            the rollout to be started
     *
     * @return the started rollout
     *
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#READY}. Only
     *             ready rollouts can be started.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    Rollout startRolloutAsync(@NotNull Rollout rollout);

    /**
     * Update rollout details.
     *
     * @param rollout
     *            rollout to be updated
     *
     * @return Rollout updated rollout
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE)
    Rollout updateRollout(@NotNull Rollout rollout);

    /**
     * Generates an empty {@link Rollout} without persisting it.
     * 
     * @return {@link Rollout} object
     */
    Rollout generateRollout();

}
