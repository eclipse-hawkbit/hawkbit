/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.AbstractRolloutGroupCreate;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;

public class JpaRolloutGroupCreate extends AbstractRolloutGroupCreate<RolloutGroupCreate>
        implements RolloutGroupCreate {

    @Override
    public JpaRolloutGroup build() {
        final JpaRolloutGroup group = new JpaRolloutGroup();

        group.setName(name);
        group.setDescription(description);
        group.setTargetFilterQuery(targetFilterQuery);

        if (targetPercentage == null) {
            targetPercentage = 100F;
        }

        group.setTargetPercentage(targetPercentage);

        if (conditions != null) {
            addSuccessAndErrorConditionsAndActions(group, conditions);
        }

        return group;
    }

    public static void addSuccessAndErrorConditionsAndActions(final JpaRolloutGroup group,
            final RolloutGroupConditions conditions) {
        addSuccessAndErrorConditionsAndActions(group, conditions.getSuccessCondition(),
                conditions.getSuccessConditionExp(), conditions.getSuccessAction(), conditions.getSuccessActionExp(),
                conditions.getErrorCondition(), conditions.getErrorConditionExp(), conditions.getErrorAction(),
                conditions.getErrorActionExp());
    }

    public static void addSuccessAndErrorConditionsAndActions(final JpaRolloutGroup group,
            final RolloutGroup.RolloutGroupSuccessCondition successCondition, final String successConditionExp,
            final RolloutGroup.RolloutGroupSuccessAction successAction, final String successActionExp,
            final RolloutGroup.RolloutGroupErrorCondition errorCondition, final String errorConditionExp,
            final RolloutGroup.RolloutGroupErrorAction errorAction, final String errorActionExp) {
        group.setSuccessCondition(successCondition);
        group.setSuccessConditionExp(successConditionExp);

        group.setSuccessAction(successAction);
        group.setSuccessActionExp(successActionExp);

        group.setErrorCondition(errorCondition);
        group.setErrorConditionExp(errorConditionExp);

        group.setErrorAction(errorAction);
        group.setErrorActionExp(errorActionExp);
    }

}
