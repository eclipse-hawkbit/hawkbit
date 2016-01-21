/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.tag;

import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;

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
public class TargetTagAssigmentResultRest {

    @JsonProperty
    private TargetsRest assignedTargets;

    @JsonProperty
    private TargetsRest unassignedTargets;

    public void setAssignedTargets(final TargetsRest assignedTargets) {
        this.assignedTargets = assignedTargets;
    }

    public TargetsRest getAssignedTargets() {
        return assignedTargets;
    }

    public void setUnassignedTargets(final TargetsRest unassignedTargets) {
        this.unassignedTargets = unassignedTargets;
    }

    public TargetsRest getUnassignedTargets() {
        return unassignedTargets;
    }

}
