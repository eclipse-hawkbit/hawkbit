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
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Success action evaluator which pauses the whole {@link Rollout}.
 */
public class PauseRolloutGroupSuccessAction
        extends AbstractPauseRolloutGroupAction<RolloutGroup.RolloutGroupSuccessAction> {

    public PauseRolloutGroupSuccessAction(final RolloutManagement rolloutManagement,
            final RolloutGroupRepository rolloutGroupRepository) {
        super(rolloutManagement, rolloutGroupRepository);
    }

    @Override
    public RolloutGroup.RolloutGroupSuccessAction getAction() {
        return RolloutGroup.RolloutGroupSuccessAction.PAUSE;
    }

    @Override
    public void exec(final Rollout rollout, final RolloutGroup rolloutGroup) {
        if (!rolloutGroupRepository
                .findByParentIdAndStatus(rolloutGroup.getId(), RolloutGroup.RolloutGroupStatus.SCHEDULED).isEmpty()) {
            // if there are still scheduled child groups, do pause the rollout, otherwise just let it in running state
            super.exec(rollout, rolloutGroup);
        }
    }
}


