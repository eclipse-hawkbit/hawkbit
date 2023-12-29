/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.AbstractRolloutUpdateCreate;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;

public class JpaRolloutCreate extends AbstractRolloutUpdateCreate<RolloutCreate> implements RolloutCreate {
    private final DistributionSetManagement distributionSetManagement;
    private boolean dynamic;

    JpaRolloutCreate(final DistributionSetManagement distributionSetManagement) {
        this.distributionSetManagement = distributionSetManagement;
    }

    public RolloutCreate dynamic(final boolean dynamic) {
        this.dynamic = dynamic;
        return this;
    }

    @Override
    public JpaRollout build() {
        final JpaRollout rollout = new JpaRollout();

        rollout.setName(name);
        rollout.setDescription(description);
        rollout.setDistributionSet(distributionSetManagement.getValidAndComplete(set));
        rollout.setTargetFilterQuery(targetFilterQuery);
        rollout.setStartAt(startAt);
        rollout.setWeight(weight);
        rollout.setDynamic(dynamic);

        if (actionType != null) {
            rollout.setActionType(actionType);
        }

        if (forcedTime != null) {
            rollout.setForcedTime(forcedTime);
        }

        return rollout;
    }
}
