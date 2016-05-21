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
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * @param pageable
     *            the page request to sort and limit the result
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<RolloutGroup> findAllRolloutGroupsWithDetailedStatus(@NotNull Long rolloutId, @NotNull Pageable pageable);

    // TODO discuss: target read perm missing?
    /**
     * 
     * Find all targets with action status by rollout group id. The action
     * status might be {@code null} if for the target within the rollout no
     * actions as been created, e.g. the target already had assigned the same
     * distribution set we do not create an action for it but the target is in
     * the result list of the rollout-group.
     * 
     * @param pageRequest
     *            the page request to sort and limit the result
     * @param rolloutGroup
     *            rollout group
     * @return {@link TargetWithActionStatus} target with action status
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<TargetWithActionStatus> findAllTargetsWithActionStatus(@NotNull PageRequest pageRequest,
            @NotNull RolloutGroup rolloutGroup);

    /**
     * Retrieves a single {@link RolloutGroup} by its ID.
     * 
     * @param rolloutGroupId
     *            the ID of the rollout group to find
     * @return the found {@link RolloutGroup} by its ID or {@code null} if it
     *         does not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    RolloutGroup findRolloutGroupById(@NotNull Long rolloutGroupId);

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout} and the given {@link Specification}.
     * 
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * @param specification
     *            the specification to filter the result set based on attributes
     *            of the {@link RolloutGroup}
     * @param pageable
     *            the page request to sort and limit the result
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<RolloutGroup> findRolloutGroupsAll(@NotNull Rollout rollout, @NotNull String rsqlParam,
            @NotNull Pageable pageable);

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
    Page<RolloutGroup> findRolloutGroupsByRolloutId(@NotNull Long rolloutId, @NotNull Pageable pageable);

    // TODO discuss: target read perm missing?
    /**
     * Get targets of specified rollout group.
     * 
     * @param rolloutGroup
     *            rollout group
     * @param page
     *            the page request to sort and limit the result
     * 
     * @return Page<Target> list of targets of a rollout group
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Target> findRolloutGroupTargets(@NotNull RolloutGroup rolloutGroup, @NotNull Pageable page);

    // TODO discuss: target read perm missing?
    /**
     * Get targets of specified rollout group.
     * 
     * @param rolloutGroup
     *            rollout group
     * @param specification
     *            the specification for filtering the targets of a rollout group
     * @param pageable
     *            the page request to sort and limit the result
     * 
     * @return Page<Target> list of targets of a rollout group
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Target> findRolloutGroupTargets(@NotNull RolloutGroup rolloutGroup, @NotNull String rsqlParam,
            @NotNull Pageable pageable);

    /**
     * Get count of targets in different status in rollout group.
     *
     * @param rolloutGroupId
     *            rollout group id
     * @return rolloutGroup with details of targets count for different statuses
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    RolloutGroup findRolloutGroupWithDetailedStatus(@NotNull Long rolloutGroupId);

    /**
     * Generates an empty {@link RolloutGroup} without persisting it.
     * 
     * @return {@link RolloutGroup} object
     */
    RolloutGroup generateRolloutGroup();
}