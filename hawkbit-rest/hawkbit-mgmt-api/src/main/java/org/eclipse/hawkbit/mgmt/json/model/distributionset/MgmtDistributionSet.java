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

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "51")
    private Long dsId;

    @JsonProperty
    @Schema(example = "1.4.2")
    private String version;

    @JsonProperty
    private List<MgmtSoftwareModule> modules = new ArrayList<>();

    @JsonProperty
    @Schema(example = "false")
    private boolean requiredMigrationStep;

    @JsonProperty
    @Schema(example = "test_default_ds_type")
    private String type;

    @JsonProperty
    @Schema(example = "OS (FW) mandatory, runtime (FW) and app (SW) optional")
    private String typeName;

    @JsonProperty
    @Schema(example = "true")
    private Boolean complete;

    @JsonProperty
    @Schema(example = "false")
    private boolean deleted;

    @JsonProperty
    @Schema(example = "true")
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
