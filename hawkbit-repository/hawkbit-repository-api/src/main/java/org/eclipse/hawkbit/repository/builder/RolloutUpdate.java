/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Builder to update an existing {@link Rollout} entry. Defines all fields that
 * can be updated.
 *
 */
public interface RolloutUpdate {
    /**
     * Set name of the {@link Rollout}
     * 
     * @param name
     *            for {@link Rollout#getName()}
     * @return updated builder instance
     */
    RolloutUpdate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * Set description of the {@link Rollout}
     * 
     * @param description
     *            for {@link Rollout#getDescription()}
     * @return updated builder instance
     */
    RolloutUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * Set ID of {@link DistributionSet} of the {@link Rollout}
     * 
     * @param setId
     *            for {@link Rollout#getDistributionSet()}
     * @return updated builder instance
     */
    RolloutUpdate set(long setId);

    /**
     * Set action type of the {@link Rollout}
     * 
     * @param actionType
     *            for {@link Rollout#getActionType()}
     * @return updated builder instance
     */
    RolloutUpdate actionType(@NotNull Action.ActionType actionType);

    /**
     * Set forcedTime of the {@link Rollout}
     * 
     * @param forcedTime
     *            for {@link Rollout#getForcedTime()}
     * @return updated builder instance
     */
    RolloutUpdate forcedTime(Long forcedTime);

    /**
     * Set weight of {@link Action}s created by the {@link Rollout}
     * 
     * @param weight
     *            for {@link Rollout#getWeight()}
     * @return updated builder instance
     */
    RolloutUpdate weight(Integer weight);

    /**
     * Set start time of the {@link Rollout}
     * 
     * @param startAt
     *            for {@link Rollout#getStartAt()}
     * @return updated builder instance
     */
    RolloutUpdate startAt(Long startAt);
}
