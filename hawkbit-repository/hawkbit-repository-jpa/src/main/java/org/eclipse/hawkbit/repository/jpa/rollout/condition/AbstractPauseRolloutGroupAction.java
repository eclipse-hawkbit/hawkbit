/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import static org.eclipse.hawkbit.context.AccessContext.asSystem;

import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Abstract class for pausing a rollout group.
 *
 * @param <T> the type of the action
 */
public abstract class AbstractPauseRolloutGroupAction<T extends Enum<T>> implements RolloutGroupActionEvaluator<T> {

    protected final RolloutManagement rolloutManagement;
    protected final RolloutGroupRepository rolloutGroupRepository;

    protected AbstractPauseRolloutGroupAction(final RolloutManagement rolloutManagement,
            final RolloutGroupRepository rolloutGroupRepository) {
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupRepository = rolloutGroupRepository;
    }

    public void exec(final Rollout rollout, final RolloutGroup rolloutGroup) {
        // Refresh latest rollout state in order to avoid cases when
        // previous group have matched error condition and paused the rollout
        // and this one tries to pause the rollout too but throws an exception
        // and rollbacks rollout processing transaction
        final Rollout refreshedRollout = rolloutManagement.get(rollout.getId());
        if (Rollout.RolloutStatus.PAUSED != refreshedRollout.getStatus()) {
            // if only the latest state is != paused then pause
            // execute as system if rollout creator has CREATE_ROLLOUT right but no HANDLE_ROLLOUT
            asSystem(() -> rolloutManagement.pauseRollout(rollout.getId()));
        }
    }
}