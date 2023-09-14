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

import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * * A json annotated rest model for TargetTagAssigmentResult to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetTagAssigmentResult {

    @JsonProperty
    private List<MgmtTarget> assignedTargets;

    @JsonProperty
    private List<MgmtTarget> unassignedTargets;

    public void setAssignedTargets(final List<MgmtTarget> assignedTargets) {
        this.assignedTargets = assignedTargets;
    }

    public List<MgmtTarget> getAssignedTargets() {
        return assignedTargets;
    }

    public void setUnassignedTargets(final List<MgmtTarget> unassignedTargets) {
        this.unassignedTargets = unassignedTargets;
    }

    public List<MgmtTarget> getUnassignedTargets() {
        return unassignedTargets;
    }

}
