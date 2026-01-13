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
public class PauseRolloutGroupErrorAction extends AbstractPauseRolloutGroupAction<RolloutGroup.RolloutGroupErrorAction> {

    public PauseRolloutGroupErrorAction(final RolloutManagement rolloutManagement,
            final RolloutGroupRepository rolloutGroupRepository) {
        super(rolloutManagement, rolloutGroupRepository);
    }

    @Override
    public RolloutGroup.RolloutGroupErrorAction getAction() {
        return RolloutGroup.RolloutGroupErrorAction.PAUSE;
    }

    @Override
    public void exec(final Rollout rollout, final RolloutGroup rolloutG) {
        // set rollout group status to error
        final JpaRolloutGroup rolloutGroup = (JpaRolloutGroup) rolloutG;
        rolloutGroup.setStatus(RolloutGroupStatus.ERROR);
        rolloutGroupRepository.save(rolloutGroup);
        // pause the rollout
        super.exec(rollout, rolloutGroup);
    }
}

