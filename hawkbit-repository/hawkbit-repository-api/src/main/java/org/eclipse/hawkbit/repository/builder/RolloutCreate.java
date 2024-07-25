/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
     * Set name
     * 
     * @param name for {@link Rollout#getName()}
     * @return updated builder instance
     */
    RolloutCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * Set description
     * 
     * @param description for {@link Rollout#getDescription()}
     * @return updated builder instance
     */
    RolloutCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * Set the {@link DistributionSet}
     * 
     * @param set for {@link Rollout#getDistributionSet()}
     * @return updated builder instance
     */
    default RolloutCreate distributionSetId(final DistributionSet set) {
        return distributionSetId(Optional.ofNullable(set).map(DistributionSet::getId).orElse(null));
    }

    /**
     * Set the id of the {@link DistributionSet}
     * 
     * @param distributionSetId for {@link Rollout#getDistributionSet()}
     * @return updated builder instance
     */
    RolloutCreate distributionSetId(long distributionSetId);

    /**
     * Set the {@link TargetFilterQuery}
     * 
     * @param targetFilterQuery for {@link Rollout#getTargetFilterQuery()}
     * @return updated builder instance
     */
    RolloutCreate targetFilterQuery(
            @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE) @NotNull String targetFilterQuery);

    /**
     * Set the {@link ActionType}
     * 
     * @param actionType for {@link Rollout#getActionType()}
     * @return updated builder instance
     */
    RolloutCreate actionType(@NotNull ActionType actionType);

    /**
     * Set the forcedTime of the resulting {@link org.eclipse.hawkbit.repository.model.Action}s
     * 
     * @param forcedTime for {@link Rollout#getForcedTime()}
     * @return updated builder instance
     */
    RolloutCreate forcedTime(Long forcedTime);

    /**
     * Set the weight of the resulting {@link org.eclipse.hawkbit.repository.model.Action}s
     * 
     * @param weight for {@link Rollout#getWeight()}
     * @return updated builder instance
     */
    RolloutCreate weight(Integer weight);

    /**
     * Set if the rollout shall be dynamic
     *
     * @param dynamic for {@link Rollout#isDynamic()}
     * @return updated builder instance
     */
    RolloutCreate dynamic(boolean dynamic);

    /**
     * Set start at
     * 
     * @param startAt for {@link Rollout#getStartAt()}
     * @return updated builder instance
     */
    RolloutCreate startAt(Long startAt);

    /**
     * @return peek on current state of {@link Rollout} in the builder
     */
    Rollout build();
}