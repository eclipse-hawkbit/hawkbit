/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for DistributionSet to RESTful API
 * representation.
 *
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSet extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    private Long dsId;

    @JsonProperty
    private String version;

    @JsonProperty
    private List<MgmtSoftwareModule> modules = new ArrayList<>();

    @JsonProperty
    private boolean requiredMigrationStep;

    @JsonProperty
    private String type;

    @JsonProperty
    private Boolean complete;

    /**
     * @return the id
     */
    public Long getDsId() {
        return dsId;
    }

    /**
     * @param id
     *            the id to set
     */
    @JsonIgnore
    public void setDsId(final Long id) {
        dsId = id;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     *            the version to set
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * @return the requiredMigrationStep
     */
    public boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    /**
     * @param requiredMigrationStep
     *            the requiredMigrationStep to set
     */
    public void setRequiredMigrationStep(final boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;
    }

    /**
     * @return the modules
     */
    public List<MgmtSoftwareModule> getModules() {
        return modules;
    }

    /**
     * @param modules
     *            the modules to set
     */
    public void setModules(final List<MgmtSoftwareModule> modules) {
        this.modules = modules;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * @return the complete
     */
    public Boolean getComplete() {
        return complete;
    }

    /**
     * @param complete
     *            the complete to set
     */
    public void setComplete(final Boolean complete) {
        this.complete = complete;
    }

}
