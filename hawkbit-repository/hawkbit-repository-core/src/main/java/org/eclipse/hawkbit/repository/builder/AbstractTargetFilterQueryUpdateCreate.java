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
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractTargetFilterQueryUpdateCreate<T> extends AbstractBaseEntityBuilder {
    @ValidString
    protected String name;

    @ValidString
    protected String query;

    protected Long distributionSetId;

    protected ActionType actionType;

    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    protected Integer weight;
    
    protected Boolean confirmationRequired;

    /**
     * Set DS ID of the {@link Action} created during auto assignment
     * 
     * @param distributionSetId
     *            of the {@link TargetFilterQuery}
     * @return this builder
     */
    public T autoAssignDistributionSet(final Long distributionSetId) {
        this.distributionSetId = distributionSetId;
        return (T) this;
    }

    public Optional<Long> getAutoAssignDistributionSetId() {
        return Optional.ofNullable(distributionSetId);
    }

    /**
     * Set {@link ActionType} of the {@link Action} created during auto
     * assignment
     * 
     * @param actionType
     *            of the {@link TargetFilterQuery}
     * @return this builder
     */
    public T autoAssignActionType(final ActionType actionType) {
        this.actionType = actionType;
        return (T) this;
    }

    /**
     * Set weight of the {@link Action} created during auto assignment
     * 
     * @param weight
     *            of the {@link TargetFilterQuery}
     * @return this builder
     */
    public T autoAssignWeight(final Integer weight) {
        this.weight = weight;
        return (T) this;
    }

    public Optional<Integer> getAutoAssignWeight() {
        return Optional.ofNullable(weight);
    }

    public Optional<ActionType> getAutoAssignActionType() {
        return Optional.ofNullable(actionType);
    }

    /**
     * Set name of the filter
     * 
     * @param name
     *            of the {@link TargetFilterQuery}
     * @return this builder
     */
    public T name(final String name) {
        this.name = StringUtils.trimWhitespace(name);
        return (T) this;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Set query used by the filter
     * 
     * @param query
     *            of the {@link TargetFilterQuery}
     * @return this builder
     */
    public T query(final String query) {
        this.query = StringUtils.trimWhitespace(query);
        return (T) this;
    }

    public Optional<String> getQuery() {
        return Optional.ofNullable(query);
    }

    /**
     * @param confirmationRequired
     *            if confirmation is required for configured auto assignment
     *            (considered with confirmation flow active)
     * @return updated builder instance
     */
    public T confirmationRequired(final boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
        return (T) this;
    }

    public Optional<Boolean> getConfirmationRequired() {
        return Optional.ofNullable(confirmationRequired);
    }
}
