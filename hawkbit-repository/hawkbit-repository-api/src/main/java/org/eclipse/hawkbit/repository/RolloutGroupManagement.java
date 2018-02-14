/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Repository management service for RolloutGroup.
 *
 */
public interface RolloutGroupManagement {

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout} with the detailed status.
     * 
     * @param pageable
     *            the page request to sort and limit the result
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * 
     * @return a page of found {@link RolloutGroup}s
     * 
     * @throws EntityNotFoundException
     *             of rollout with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<RolloutGroup> findByRolloutWithDetailedStatus(@NotNull Pageable pageable, long rolloutId);

    /**
     * 
     * Find all targets with action status by rollout group id. The action
     * status might be {@code null} if for the target within the rollout no
     * actions as been created, e.g. the target already had assigned the same
     * distribution set we do not create an action for it but the target is in
     * the result list of the rollout-group.
     * 
     * @param pageable
     *            the page request to sort and limit the result
     * @param rolloutGroupId
     *            rollout group
     * @return {@link TargetWithActionStatus} target with action status
     * @throws EntityNotFoundException
     *             if rollout group with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    Page<TargetWithActionStatus> findAllTargetsOfRolloutGroupWithActionStatus(@NotNull Pageable pageable,
            long rolloutGroupId);

    /**
     * Retrieves a single {@link RolloutGroup} by its ID.
     * 
     * @param rolloutGroupId
     *            the ID of the rollout group to find
     * @return the found {@link RolloutGroup} by its ID or {@code null} if it
     *         does not exists
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Optional<RolloutGroup> get(long rolloutGroupId);

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout} and the an rsql filter.
     * 
     * @param pageable
     *            the page request to sort and limit the result
     * @param rolloutId
     *            the rollout to filter the {@link RolloutGroup}s
     * @param rsqlParam
     *            the specification to filter the result set based on attributes
     *            of the {@link RolloutGroup}
     * 
     * @return a page of found {@link RolloutGroup}s
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<RolloutGroup> findByRolloutAndRsql(@NotNull Pageable pageable, long rolloutId,
            @NotNull String rsqlParam);

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout}.
     * 
     * @param pageable
     *            the page request to sort and limit the result
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * 
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<RolloutGroup> findByRollout(@NotNull Pageable pageable, long rolloutId);

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout}.
     * 
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * @param pageable
     *            the page request to sort and limit the result
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    long countByRollout(long rolloutId);

    /**
     * Get targets of specified rollout group.
     * 
     * @param pageable
     *            the page request to sort and limit the result
     * @param rolloutGroupId
     *            rollout group
     * 
     * @return Page<Target> list of targets of a rollout group
     * 
     * @throws EntityNotFoundException
     *             if group with ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    Page<Target> findTargetsOfRolloutGroup(@NotNull Pageable pageable, long rolloutGroupId);

    /**
     * Get targets of specified rollout group.
     * 
     * @param pageable
     *            the page request to sort and limit the result
     * @param rolloutGroupId
     *            rollout group
     * @param rsqlParam
     *            the specification for filtering the targets of a rollout group
     * 
     * @return Page<Target> list of targets of a rollout group
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    Page<Target> findTargetsOfRolloutGroupByRsql(@NotNull Pageable pageable, long rolloutGroupId,
            @NotNull String rsqlParam);

    /**
     * Get {@link RolloutGroup} by Id.
     *
     * @param rolloutGroupId
     *            rollout group id
     * @return rolloutGroup with details of targets count for different statuses
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Optional<RolloutGroup> getWithDetailedStatus(long rolloutGroupId);

    /**
     * Count targets of rollout group.
     * 
     * @param rolloutGroupId
     *            the rollout group id for the count
     * @return the target rollout group count
     * 
     * @throws EntityNotFoundException
     *             if rollout group with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    long countTargetsOfRolloutsGroup(long rolloutGroupId);
}
