/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;

import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Represents the handler service for creating, deleting, and starting a Rollout
 */
@FunctionalInterface
public interface RolloutExecutor {

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
    void execute(Rollout rollout);
}
