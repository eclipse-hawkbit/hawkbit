/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.AbstractRolloutUpdateCreate;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.DistributionSet;

public class JpaRolloutCreate extends AbstractRolloutUpdateCreate<RolloutCreate> implements RolloutCreate {
    private final DistributionSetManagement distributionSetManagement;

    JpaRolloutCreate(final DistributionSetManagement distributionSetManagement) {
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public JpaRollout build() {
        final JpaRollout rollout = new JpaRollout();

        rollout.setName(name);
        rollout.setDescription(description);
        rollout.setDistributionSet(findDistributionSetAndThrowExceptionIfNotFound(set));
        rollout.setTargetFilterQuery(targetFilterQuery);
        rollout.setStartAt(startAt);

        if (actionType != null) {
            rollout.setActionType(actionType);
        }

        if (forcedTime != null) {
            rollout.setForcedTime(forcedTime);
        }

        return rollout;
    }

    private DistributionSet findDistributionSetAndThrowExceptionIfNotFound(final Long setId) {
        return distributionSetManagement.get(setId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, setId));
    }
}
