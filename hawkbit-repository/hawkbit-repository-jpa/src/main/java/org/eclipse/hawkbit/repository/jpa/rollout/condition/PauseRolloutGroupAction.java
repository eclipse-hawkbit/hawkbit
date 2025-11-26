/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;

/**
 * Error action evaluator which pauses the whole {@link Rollout} and sets the
 * current {@link RolloutGroup} to error.
 */
public class PauseRolloutGroupAction implements RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupErrorAction> {

    private final RolloutManagement rolloutManagement;
    private final RolloutGroupRepository rolloutGroupRepository;

    public PauseRolloutGroupAction(final RolloutManagement rolloutManagement,
            final RolloutGroupRepository rolloutGroupRepository) {
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupRepository = rolloutGroupRepository;
    }

    @Override
    public RolloutGroup.RolloutGroupErrorAction getAction() {
        return RolloutGroup.RolloutGroupErrorAction.PAUSE;
    }

    @Override
    public void exec(final Rollout rollout, final RolloutGroup rolloutG) {

        final JpaRolloutGroup rolloutGroup = (JpaRolloutGroup) rolloutG;

        AccessContext.asSystem(() -> {
            rolloutGroup.setStatus(RolloutGroupStatus.ERROR);
            rolloutGroupRepository.save(rolloutGroup);
            /*
                Refresh latest rollout state in order to escape cases when
                previous group have matched error condition and paused the rollout
                and this one tries to pause the rollout too but throws an exception
                and rollbacks rollout processing transaction
            */
            final Rollout refreshedRollout = rolloutManagement.get(rollout.getId());
            if (Rollout.RolloutStatus.PAUSED != refreshedRollout.getStatus()) {
                // if only the latest state is != paused then pause
                rolloutManagement.pauseRollout(rollout.getId());
            }
        });
    }
}
