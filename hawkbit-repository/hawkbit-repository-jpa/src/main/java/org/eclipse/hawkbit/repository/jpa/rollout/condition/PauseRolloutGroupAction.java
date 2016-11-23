/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Error action evaluator which pauses the whole {@link Rollout} and sets the
 * current {@link RolloutGroup} to error.
 */
@Component("pauseRolloutGroupAction")
public class PauseRolloutGroupAction implements RolloutGroupActionEvaluator {

    @Autowired
    private RolloutManagement rolloutManagement;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Override
    public boolean verifyExpression(final String expression) {
        return true;
    }

    @Override
    public void eval(final Rollout rollout, final RolloutGroup rolloutG, final String expression) {
        final JpaRolloutGroup rolloutGroup = (JpaRolloutGroup) rolloutG;

        systemSecurityContext.runAsSystem(() -> {
            rolloutGroup.setStatus(RolloutGroupStatus.ERROR);
            rolloutGroupRepository.save(rolloutGroup);
            rolloutManagement.pauseRollout(rollout.getId());
            return null;
        });
    }
}
