/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;

/**
 * Holds properties for {@link Action}
 */
public class ActionProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Action.ActionType actionType;
    private Action.Status status;
    private String tenant;
    private boolean maintenanceWindowAvailable;

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
        this.status = action.getStatus();
        this.tenant = action.getTenant();
        this.maintenanceWindowAvailable = action.isMaintenanceWindowAvailable();
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

    public void setStatus(Action.Status status) {
        this.status = status;
    }
}
