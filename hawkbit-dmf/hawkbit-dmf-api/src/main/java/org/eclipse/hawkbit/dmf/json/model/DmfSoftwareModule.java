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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JSON representation of a software module.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfSoftwareModule {

    private final Long moduleId;
    private final String moduleType;
    private final String moduleVersion;
    private final Boolean encrypted;
    private final List<DmfArtifact> artifacts;
    private final List<DmfMetadata> metadata;

    @JsonCreator
    public DmfSoftwareModule(
            @JsonProperty("moduleId") final Long moduleId,
            @JsonProperty("moduleType") final String moduleType,
            @JsonProperty("moduleVersion") final String moduleVersion,
            @JsonProperty("encrypted") final Boolean encrypted,
            @JsonProperty("artifacts") final List<DmfArtifact> artifacts,
            @JsonProperty("metadata") final List<DmfMetadata> metadata) {
        this.moduleId = moduleId;
        this.moduleType = moduleType;
        this.moduleVersion = moduleVersion;
        this.encrypted = encrypted;
        this.artifacts = artifacts == null ? Collections.emptyList() : Collections.unmodifiableList(artifacts);
        this.metadata = metadata == null || metadata.isEmpty() ? null : metadata;
    }
}