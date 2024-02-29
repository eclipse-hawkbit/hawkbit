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
import lombok.Data;

/**
 * JSON representation of a software module.
 */
@Data
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

    public List<DmfArtifact> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(artifacts);
    }

    public void setMetadata(final List<DmfMetadata> metadata) {
        if (metadata != null && !metadata.isEmpty()) {
            this.metadata = metadata;
        }
    }
}