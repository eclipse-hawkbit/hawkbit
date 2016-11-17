/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;

/**
 * Builder to build easily the {@link RolloutGroupConditions}.
 *
 */
public class RolloutGroupConditionBuilder {
    private final RolloutGroupConditions conditions = new RolloutGroupConditions();

    /**
     * @return completed {@link RolloutGroupConditions}.
     */
    public RolloutGroupConditions build() {
        return conditions;
    }

    /**
     * Sets the finish condition and expression on the builder.
     *
     * @param condition
     *            the finish condition
     * @param expression
     *            the finish expression
     * @return the builder itself
     */
    public RolloutGroupConditionBuilder successCondition(final RolloutGroupSuccessCondition condition,
            final String expression) {
        conditions.setSuccessCondition(condition);
        conditions.setSuccessConditionExp(expression);
        return this;
    }

    /**
     * Sets the success action and expression on the builder.
     *
     * @param action
     *            the success action
     * @param expression
     *            the error expression
     * @return the builder itself
     */
    public RolloutGroupConditionBuilder successAction(final RolloutGroupSuccessAction action, final String expression) {
        conditions.setSuccessAction(action);
        conditions.setSuccessActionExp(expression);
        return this;
    }

    /**
     * Sets the error condition and expression on the builder.
     *
     * @param condition
     *            the error condition
     * @param expression
     *            the error expression
     * @return the builder itself
     */
    public RolloutGroupConditionBuilder errorCondition(final RolloutGroupErrorCondition condition,
            final String expression) {
        conditions.setErrorCondition(condition);
        conditions.setErrorConditionExp(expression);
        return this;
    }

    /**
     * Sets the error action and expression on the builder.
     *
     * @param action
     *            the error action
     * @param expression
     *            the error expression
     * @return the builder itself
     */
    public RolloutGroupConditionBuilder errorAction(final RolloutGroupErrorAction action, final String expression) {
        conditions.setErrorAction(action);
        conditions.setErrorActionExp(expression);
        return this;
    }

    /**
     * Sets condition defaults.
     * 
     * @return the builder itself
     */
    public RolloutGroupConditionBuilder withDefaults() {
        successCondition(RolloutGroupSuccessCondition.THRESHOLD, "50");
        successAction(RolloutGroupSuccessAction.NEXTGROUP, "");
        errorCondition(RolloutGroupErrorCondition.THRESHOLD, "50");
        errorAction(RolloutGroupErrorAction.PAUSE, "");
        return this;
    }
}
