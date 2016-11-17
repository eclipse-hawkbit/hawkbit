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
            group.setSuccessCondition(conditions.getSuccessCondition());
            group.setSuccessConditionExp(conditions.getSuccessConditionExp());

            group.setSuccessAction(conditions.getSuccessAction());
            group.setSuccessActionExp(conditions.getSuccessActionExp());

            group.setErrorCondition(conditions.getErrorCondition());
            group.setErrorConditionExp(conditions.getErrorConditionExp());

            group.setErrorAction(conditions.getErrorAction());
            group.setErrorActionExp(conditions.getErrorActionExp());
        }

        return group;
    }

}
