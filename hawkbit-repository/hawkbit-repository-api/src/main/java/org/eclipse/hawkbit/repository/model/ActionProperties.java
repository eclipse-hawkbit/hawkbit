/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

public class ActionProperties {

    private Long id;
    private boolean downloadOnly;
    private String tenant;
    private boolean maintenanceWindowAvailable;

    public ActionProperties() {
    }

    public ActionProperties(Action action) {
        this.id = action.getId();
        this.downloadOnly = action.getActionType().equals(Action.ActionType.DOWNLOAD_ONLY);
        this.tenant = action.getTenant();
        this.maintenanceWindowAvailable = action.isMaintenanceWindowAvailable();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setDownloadOnly(boolean downloadOnly) {
        this.downloadOnly = downloadOnly;
    }

    public boolean isDownloadOnly() {
        return downloadOnly;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }

    public void setMaintenanceWindowAvailable(boolean maintenanceWindowAvailable) {
        this.maintenanceWindowAvailable = maintenanceWindowAvailable;
    }

    public boolean isMaintenanceWindowAvailable() {
        return maintenanceWindowAvailable;
    }
}
