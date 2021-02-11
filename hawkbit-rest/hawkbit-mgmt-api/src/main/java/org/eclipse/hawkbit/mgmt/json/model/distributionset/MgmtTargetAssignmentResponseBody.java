/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import java.util.List;
import java.util.Objects;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response Body of Target for assignment operations.
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetAssignmentResponseBody extends RepresentationModel<MgmtTargetAssignmentResponseBody> {

    private int alreadyAssigned;
    private List<MgmtActionId> assignedActions;

    /**
     * @return the count of assigned targets
     */
    @JsonProperty("assigned")
    public int getAssigned() {
        return assignedActions == null ? 0 : assignedActions.size();
    }

    /**
     * @return the alreadyAssigned
     */
    public int getAlreadyAssigned() {
        return alreadyAssigned;
    }

    /**
     * @param alreadyAssigned
     *            the alreadyAssigned to set
     */
    public void setAlreadyAssigned(final int alreadyAssigned) {
        this.alreadyAssigned = alreadyAssigned;
    }

    /**
     * @return the total
     */
    @JsonProperty("total")
    public int getTotal() {
        return getAssigned() + alreadyAssigned;
    }

    /**
     * @return the assignedActions
     */
    public List<MgmtActionId> getAssignedActions() {
        return assignedActions;
    }

    /**
     * @param assignedActions
     *            the assigned actions to set
     */
    public void setAssignedActions(final List<MgmtActionId> assignedActions) {
        this.assignedActions = assignedActions;
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj) && this.getClass().isInstance(obj)
                && ((MgmtTargetAssignmentResponseBody) obj).getAlreadyAssigned() == alreadyAssigned
                && Objects.equals(((MgmtTargetAssignmentResponseBody) obj).getAssignedActions(), assignedActions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), alreadyAssigned, assignedActions);
    }
}
