/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * Holds properties for {@link Action}
 */
public class ActionProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Action.ActionType actionType;
    private String tenant;
    private boolean maintenanceWindowAvailable;

    private Action.Status status;

    public ActionProperties() {
    }

    /**
     * Constructor
     * @param action
     *              the action to populate the properties from
     */
    public ActionProperties(final Action action) {
        this.id = action.getId();
        this.actionType = action.getActionType();
        this.tenant = action.getTenant();
        this.maintenanceWindowAvailable = action.isMaintenanceWindowAvailable();
        this.status = action.getStatus();
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }

    public void setMaintenanceWindowAvailable(final boolean maintenanceWindowAvailable) {
        this.maintenanceWindowAvailable = maintenanceWindowAvailable;
    }

    public boolean isMaintenanceWindowAvailable() {
        return maintenanceWindowAvailable;
    }

    public Action.ActionType getActionType() {
        return actionType;
    }

    public void setActionType(final Action.ActionType actionType) {
        this.actionType = actionType;
    }

    public Action.Status getStatus() {
        return status;
    }

    @JsonIgnore
    public boolean isWaitingConfirmation() {
        return status == Action.Status.WAIT_FOR_CONFIRMATION;
    }
}
