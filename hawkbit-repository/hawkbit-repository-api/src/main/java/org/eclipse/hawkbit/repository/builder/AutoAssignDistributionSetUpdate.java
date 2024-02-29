/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Builder to update the auto assign {@link DistributionSet} of a
 * {@link TargetFilterQuery} entry. Defines all fields that can be updated.
 */
@Getter
@EqualsAndHashCode
@ToString
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
}