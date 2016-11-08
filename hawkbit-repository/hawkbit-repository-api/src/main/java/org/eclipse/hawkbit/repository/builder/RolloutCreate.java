/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Builder to create a new {@link Rollout} entry. Defines all fields that can be
 * set at creation time. Other fields are set by the repository automatically,
 * e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface RolloutCreate {
    /**
     * @param name
     *            for {@link Rollout#getName()}
     * @return updated builder instance
     */
    RolloutCreate name(@NotEmpty String name);

    /**
     * @param description
     *            for {@link Rollout#getDescription()}
     * @return updated builder instance
     */
    RolloutCreate description(String description);

    /**
     * @param set
     *            for {@link Rollout#getDistributionSet()}
     * @return updated builder instance
     */
    default RolloutCreate set(final DistributionSet set) {
        return set(Optional.ofNullable(set).map(DistributionSet::getId).orElse(null));
    }

    /**
     * @param setId
     *            for {@link Rollout#getDistributionSet()}
     * @return updated builder instance
     */
    RolloutCreate set(long setId);

    /**
     * @param targetFilterQuery
     *            for {@link Rollout#getTargetFilterQuery()}
     * @return updated builder instance
     */
    RolloutCreate targetFilterQuery(@NotEmpty String targetFilterQuery);

    /**
     * @param actionType
     *            for {@link Rollout#getActionType()}
     * @return updated builder instance
     */
    RolloutCreate actionType(@NotNull ActionType actionType);

    /**
     * @param forcedTime
     *            for {@link Rollout#getForcedTime()}
     * @return updated builder instance
     */
    RolloutCreate forcedTime(Long forcedTime);

    /**
     * @return peek on current state of {@link Rollout} in the builder
     */
    Rollout build();

}
