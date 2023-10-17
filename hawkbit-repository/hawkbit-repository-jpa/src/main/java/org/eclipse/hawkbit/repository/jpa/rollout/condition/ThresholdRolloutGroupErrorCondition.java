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

import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates if the {@link RolloutGroup#getErrorConditionExp()} is reached.
 */
public class ThresholdRolloutGroupErrorCondition
        implements RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupErrorCondition> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThresholdRolloutGroupErrorCondition.class);

    private final ActionRepository actionRepository;

    public ThresholdRolloutGroupErrorCondition(final ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Override
    public RolloutGroup.RolloutGroupErrorCondition getCondition() {
        return RolloutGroup.RolloutGroupErrorCondition.THRESHOLD;
    }

    @Override
    public boolean eval(final Rollout rollout, final RolloutGroup rolloutGroup, final String expression) {
        final Long totalGroup = actionRepository.countByRolloutAndRolloutGroup((JpaRollout) rollout,
                (JpaRolloutGroup) rolloutGroup);
        final Long error = actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(rollout.getId(),
                rolloutGroup.getId(), Action.Status.ERROR);
        try {
            final Integer threshold = Integer.valueOf(expression);

            if (totalGroup == 0) {
                // in case e.g. targets has been deleted we don't have any
                // actions left for this group, so the group is finished
                return false;
            }

            // calculate threshold
            return ((float) error / (float) totalGroup) > ((float) threshold / 100F);
        } catch (final NumberFormatException e) {
            LOGGER.error("Cannot evaluate condition expression " + expression, e);
            return false;
        }
    }

}
