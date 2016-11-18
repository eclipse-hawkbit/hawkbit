/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Verifies {@link RolloutGroup#getErrorConditionExp()}.
 */
@FunctionalInterface
public interface RolloutGroupConditionEvaluator {

    default boolean verifyExpression(final String expression) {
        // percentage value between 0 and 100
        try {
            final Integer value = Integer.valueOf(expression);
            return value >= 0 && value <= 100;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    boolean eval(Rollout rollout, RolloutGroup rolloutGroup, final String expression);
}
