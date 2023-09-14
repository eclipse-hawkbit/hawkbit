/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.targetfilter;

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body of DistributionSet Id and Action Type for target filter auto
 * assignment operation.
 */
public class MgmtDistributionSetAutoAssignment extends MgmtId {

    @JsonProperty(required = false)
    private MgmtActionType type;

    @JsonProperty(required = false)
    private Integer weight;

    @JsonProperty(required = false)
    private Boolean confirmationRequired;

    public MgmtActionType getType() {
        return type;
    }

    public void setType(final MgmtActionType type) {
        this.type = type;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(final Integer weight) {
        this.weight = weight;
    }

    public Boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(final Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
