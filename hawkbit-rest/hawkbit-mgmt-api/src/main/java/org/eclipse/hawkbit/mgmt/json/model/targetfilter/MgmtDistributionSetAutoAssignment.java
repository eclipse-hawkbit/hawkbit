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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body of DistributionSet Id and Action Type for target filter auto
 * assignment operation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MgmtDistributionSetAutoAssignment extends MgmtId {

    @JsonProperty
    private MgmtActionType type;
    @JsonProperty
    private Integer weight;
    @JsonProperty
    private Boolean confirmationRequired;
}