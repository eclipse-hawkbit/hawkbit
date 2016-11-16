/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * Builder to update an existing {@link Rollout} entry. Defines all fields that
 * can be updated.
 *
 */
public interface RolloutUpdate {
    /**
     * @param name
     *            for {@link Rollout#getName()}
     * @return updated builder instance
     */
    RolloutUpdate name(@NotEmpty String name);

    /**
     * @param description
     *            for {@link Rollout#getDescription()}
     * @return updated builder instance
     */
    RolloutUpdate description(String description);

    /**
     * @param set
     *            for {@link Rollout#getDistributionSet()}
     * @return updated builder instance
     */
    default RolloutUpdate set(final DistributionSet set) {
        return set(Optional.ofNullable(set).map(DistributionSet::getId).orElse(null));
    }

    /**
     * @param setId
     *            for {@link Rollout#getDistributionSet()}
     * @return updated builder instance
     */
    RolloutUpdate set(long setId);

    /**
     * @param actionType
     *            for {@link Rollout#getActionType()}
     * @return updated builder instance
     */
    RolloutUpdate actionType(@NotNull Action.ActionType actionType);

    /**
     * @param forcedTime
     *            for {@link Rollout#getForcedTime()}
     * @return updated builder instance
     */
    RolloutUpdate forcedTime(Long forcedTime);

}
