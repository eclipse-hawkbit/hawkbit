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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Builder to create a new {@link RolloutGroup} entry. Defines all fields that
 * can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 */
public interface RolloutGroupCreate {
    /**
     * @param name for {@link Rollout#getName()}
     * @return updated builder instance
     */
    RolloutGroupCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description for {@link Rollout#getDescription()}
     * @return updated builder instance
     */
    RolloutGroupCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param targetFilterQuery for {@link Rollout#getTargetFilterQuery()}
     * @return updated builder instance
     */
    RolloutGroupCreate targetFilterQuery(
            @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE) @NotNull String targetFilterQuery);

    /**
     * @param targetPercentage the percentage of matching Targets that should be assigned to this Group
     * @return updated builder instance
     */
    RolloutGroupCreate targetPercentage(Float targetPercentage);

    /**
     * @param conditions as created by {@link RolloutGroupConditionBuilder}.
     * @return updated builder instance
     */
    RolloutGroupCreate conditions(RolloutGroupConditions conditions);

    /**
     * @param confirmationRequired if confirmation is required for this rollout group (considered
     *            with confirmation flow active)
     * @return updated builder instance
     */
    RolloutGroupCreate confirmationRequired(boolean confirmationRequired);

    /**
     * @return peek on current state of {@link RolloutGroup} in the builder
     */
    RolloutGroup build();
}