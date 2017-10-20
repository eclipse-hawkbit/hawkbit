/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON annotated REST model for {@link OfflineUpdateData}. It represents all
 * data required for recording and persisting offline update information for a
 * set of targets.
 */
@JsonInclude(Include.NON_NULL)
public class OfflineUpdateData {

    @JsonProperty(value = "controllerIds", required = true)
    private List<String> controllerIds = new ArrayList<>();

    @JsonProperty(value = "softwareModules", required = true)
    private List<SoftwareModuleInfo> softwareModules = new ArrayList<>();

    @JsonProperty(value = "migrationStepRequired")
    private boolean migrationStepRequired;

    /**
     * Returns the list of controller Ids for which offline update is performed.
     *
     * @return List of controllerIds.
     */
    public List<String> getControllerIds() {
        return controllerIds;
    }

    /**
     * Sets the list the controller Ids.
     *
     * @param {@link
     *            List} of controllerIds.
     */
    public void setControllerIds(List<String> controllerIds) {
        this.controllerIds = controllerIds;
    }

    /**
     * Returns {@code true} in case this offline update is marked as a necessary
     * migration step.
     *
     * @return migrationStepRequired.
     */
    public boolean isMigrationStepRequired() {
        return migrationStepRequired;
    }

    /**
     * Sets whether migration step is required ({@code true}) or not
     * ({@code false}) for this offline software update.
     *
     * @param migrationStepRequired.
     */
    public void setMigrationStepRequired(boolean migrationStepRequired) {
        this.migrationStepRequired = migrationStepRequired;
    }

    /**
     * Returns the list of {@link SoftwareModuleInfo}s for the offline update.
     * Each {@link SoftwareModuleInfo} represents a module that has been
     * installed or updated offline.
     *
     * @return List of {@link SoftwareModuleInfo}s.
     */
    public List<SoftwareModuleInfo> getSoftwareModules() {
        return softwareModules;
    }

    /**
     * Sets a list of {@link SoftwareModuleInfo}s for the offline update. Each
     * {@link SoftwareModuleInfo} represents a module that has been installed or
     * updated offline.
     *
     * @param List
     *            of {@link SoftwareModuleInfo}s.
     */
    public void setSoftwareModules(List<SoftwareModuleInfo> softwareModules) {
        this.softwareModules = softwareModules;
    }
}
