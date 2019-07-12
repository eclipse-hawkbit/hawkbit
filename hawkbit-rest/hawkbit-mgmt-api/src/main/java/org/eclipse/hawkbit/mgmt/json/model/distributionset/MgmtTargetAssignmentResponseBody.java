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
public class MgmtTargetAssignmentResponseBody {

    private List<MgmtActionId> assignedActions;
    private List<MgmtActionId> alreadyAssignedActions;
    private int assigned;
    private int alreadyAssigned;
    /**
     * @return the assigned
     */
    public int getAssigned() {
        return assigned;
    }

    /**
     * @param assigned
     *            the assigned to set
     */
    public void setAssigned(final int assigned) {
        this.assigned = assigned;
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
        return assigned + alreadyAssigned;
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

    /**
     * @return the alreadyAssignedActions
     */
    public List<MgmtActionId> getAlreadyAssignedActions() {
        return alreadyAssignedActions;
    }

    /**
     * @param alreadyAssignedActions
     *            the alreadyAssignedActions to set
     */
    public void setAlreadyAssignedActions(final List<MgmtActionId> alreadyAssignedActions) {
        this.alreadyAssignedActions = alreadyAssignedActions;
    }
}
