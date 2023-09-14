/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.tag;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * * A json annotated rest model for DSAssigmentResult to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetTagAssigmentResult {

    @JsonProperty
    private List<MgmtDistributionSet> assignedDistributionSets;

    @JsonProperty
    private List<MgmtDistributionSet> unassignedDistributionSets;

    public List<MgmtDistributionSet> getAssignedDistributionSets() {
        return assignedDistributionSets;
    }

    public List<MgmtDistributionSet> getUnassignedDistributionSets() {
        return unassignedDistributionSets;
    }

    public void setAssignedDistributionSets(final List<MgmtDistributionSet> assignedDistributionSets) {
        this.assignedDistributionSets = assignedDistributionSets;
    }

    public void setUnassignedDistributionSets(final List<MgmtDistributionSet> unassignedDistributionSets) {
        this.unassignedDistributionSets = unassignedDistributionSets;
    }

}
