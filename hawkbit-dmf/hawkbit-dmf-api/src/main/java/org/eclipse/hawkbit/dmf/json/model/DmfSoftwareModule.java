/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation of a software module.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfSoftwareModule {

    @JsonProperty
    private Long moduleId;
    @JsonProperty
    private String moduleType;
    @JsonProperty
    private String moduleVersion;
    @JsonProperty
    private Boolean encrypted;
    @JsonProperty
    private List<DmfArtifact> artifacts;
    @JsonProperty
    private List<DmfMetadata> metadata;

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(final String moduleType) {
        this.moduleType = moduleType;
    }

    public String getModuleVersion() {
        return moduleVersion;
    }

    public void setModuleVersion(final String moduleVersion) {
        this.moduleVersion = moduleVersion;
    }

    public List<DmfArtifact> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(artifacts);
    }

    public Long getModuleId() {
        return moduleId;
    }

    @JsonIgnore
    public void setModuleId(final Long moduleId) {
        this.moduleId = moduleId;
    }

    public void setArtifacts(final List<DmfArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    public List<DmfMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(final List<DmfMetadata> metadata) {
        if (metadata != null && !metadata.isEmpty()) {
            this.metadata = metadata;
        }
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(final Boolean encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public String toString() {
        return String.format(
                "DmfSoftwareModule [moduleId=%d, moduleType='%s', moduleVersion='%s', encrypted='%s' artifacts=%s, metadata=%s]",
                moduleId, moduleType, moduleVersion, encrypted, artifacts, metadata);
    }
}
