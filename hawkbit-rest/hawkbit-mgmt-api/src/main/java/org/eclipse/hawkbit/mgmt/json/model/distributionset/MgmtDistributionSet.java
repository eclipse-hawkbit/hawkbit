/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
    private String typeName;

    @JsonProperty
    private Boolean complete;

    @JsonProperty
    private boolean deleted;

    @JsonProperty
    private boolean valid;

    public boolean isValid() {
        return valid;
    }

    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public Long getDsId() {
        return dsId;
    }

    @JsonIgnore
    public void setDsId(final Long id) {
        dsId = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    public void setRequiredMigrationStep(final boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;
    }

    public List<MgmtSoftwareModule> getModules() {
        return modules;
    }

    public void setModules(final List<MgmtSoftwareModule> modules) {
        this.modules = modules;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(final Boolean complete) {
        this.complete = complete;
    }

}
