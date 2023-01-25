/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Builder to update the auto assign {@link DistributionSet} of a
 * {@link TargetFilterQuery} entry. Defines all fields that can be updated.
 */
public class AutoAssignDistributionSetUpdate {
    private final long targetFilterId;
    private Long dsId;
    private ActionType actionType;

    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    private Integer weight;

    private Boolean confirmationRequired;

    /**
     * Constructor
     * 
     * @param targetFilterId
     *            ID of {@link TargetFilterQuery} to update
     */
    public AutoAssignDistributionSetUpdate(final long targetFilterId) {
        this.targetFilterId = targetFilterId;
    }

    /**
     * Specify {@link DistributionSet}
     * 
     * @param dsId
     *            ID of the {@link DistributionSet}
     * @return updated builder instance
     */
    public AutoAssignDistributionSetUpdate ds(final Long dsId) {
        this.dsId = dsId;
        return this;
    }

    /**
     * Specify {@link DistributionSet}
     * 
     * @param actionType
     *            {@link ActionType} used for the auto assignment
     * @return updated builder instance
     */
    public AutoAssignDistributionSetUpdate actionType(final ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    /**
     * Specify weight of resulting {@link Action}
     * 
     * @param weight
     *            weight used for the auto assignment
     * @return updated builder instance
     */
    public AutoAssignDistributionSetUpdate weight(final Integer weight) {
        this.weight = weight;
        return this;
    }

    /**
     * Specify initial confirmation state of resulting {@link Action}
     *
     * @param confirmationRequired
     *            if confirmation is required for this auto assignment (considered
     *            with confirmation flow active)
     * @return updated builder instance
     */
    public AutoAssignDistributionSetUpdate confirmationRequired(final boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
        return this;
    }

    public Long getDsId() {
        return dsId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public Integer getWeight() {
        return weight;
    }

    public Boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public long getTargetFilterId() {
        return targetFilterId;
    }

}
