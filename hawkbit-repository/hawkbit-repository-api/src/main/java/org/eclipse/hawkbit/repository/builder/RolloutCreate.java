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
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

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
    RolloutCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description
     *            for {@link Rollout#getDescription()}
     * @return updated builder instance
     */
    RolloutCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

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
    RolloutCreate targetFilterQuery(
            @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE) @NotNull String targetFilterQuery);

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
     * @param startAt
     *            for {@link Rollout#getStartAt()}
     * @return updated builder instance
     */
    RolloutCreate startAt(Long startAt);

    /**
     * @return peek on current state of {@link Rollout} in the builder
     */
    Rollout build();

}
