/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
