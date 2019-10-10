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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractRolloutUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    protected Long set;

    @ValidString
    protected String targetFilterQuery;

    protected ActionType actionType;
    protected Long forcedTime;
    protected Long startAt;

    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    protected Integer weight;

    /**
     * {@link DistributionSet} of rollout
     * 
     * @param set
     *            ID of the set
     * @return this builder
     */
    public T set(final long set) {
        this.set = set;
        return (T) this;
    }

    /**
     * Filter of the rollout
     * 
     * @param targetFilterQuery
     *            query
     * @return this builder
     */
    public T targetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = StringUtils.trimWhitespace(targetFilterQuery);
        return (T) this;
    }

    /**
     * {@link ActionType} used for {@link Action}s
     * 
     * @param actionType
     *            type
     * @return this builder
     */
    public T actionType(final ActionType actionType) {
        this.actionType = actionType;
        return (T) this;
    }

    /**
     * forcedTime used for {@link Action}s
     * 
     * @param forcedTime
     *            time
     * @return this builder
     */
    public T forcedTime(final Long forcedTime) {
        this.forcedTime = forcedTime;
        return (T) this;
    }

    /**
     * weight used for {@link Action}s
     * 
     * @param weight
     *            weight
     * @return this builder
     */
    public T weight(final Integer weight) {
        this.weight = weight;
        return (T) this;
    }

    /**
     * Set start of the Rollout
     * 
     * @param startAt
     *            start time point
     * @return this builder
     */
    public T startAt(final Long startAt) {
        this.startAt = startAt;
        return (T) this;
    }

    public Optional<Long> getSet() {
        return Optional.ofNullable(set);
    }

    public Optional<ActionType> getActionType() {
        return Optional.ofNullable(actionType);
    }

    public Optional<Long> getForcedTime() {
        return Optional.ofNullable(forcedTime);
    }

    public Optional<Integer> getWeight() {
        return Optional.ofNullable(weight);
    }

    public Optional<Long> getStartAt() {
        return Optional.ofNullable(startAt);
    }
}
