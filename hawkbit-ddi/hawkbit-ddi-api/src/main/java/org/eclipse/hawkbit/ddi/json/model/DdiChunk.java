/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Deployment chunks.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiChunk {

    @NotNull
    @Schema(description = "Type of the chunk, e.g. firmware, bundle, app. In update server mapped to Software Module Type")
    private final String part;

    @NotNull
    @Schema(description = "Software version of the chunk", example = "1.2.0")
    private final String version;

    @NotNull
    @Schema(description = "Name of the chunk")
    private final String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "If encrypted")
    private final Boolean encrypted;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "List of artifacts")
    private final List<DdiArtifact> artifacts;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Meta data of the respective software module that has been marked with 'target visible'")
    private final List<DdiMetadata> metadata;

    /**
     * Constructor.
     *
     * @param part of the deployment chunk
     * @param version of the artifact
     * @param name of the artifact
     * @param encrypted if artifacts are encrypted
     * @param artifacts download information
     * @param metadata optional as additional information for the target/device
     */
    @JsonCreator
    public DdiChunk(
            @JsonProperty("part") final String part,
            @JsonProperty("version") final String version,
            @JsonProperty("name") final String name,
            @JsonProperty("encrypted") final Boolean encrypted,
            @JsonProperty("artifacts") final List<DdiArtifact> artifacts,
            @JsonProperty("metadata") final List<DdiMetadata> metadata) {
        this.part = part;
        this.version = version;
        this.name = name;
        this.encrypted = encrypted;
        this.artifacts = artifacts;
        this.metadata = metadata;
    }

    public List<DdiArtifact> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(artifacts);
    }
}