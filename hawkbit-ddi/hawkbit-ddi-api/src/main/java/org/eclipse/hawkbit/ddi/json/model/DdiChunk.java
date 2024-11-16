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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Deployment chunks.
 */
@NoArgsConstructor // needed for json create
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiChunk {

    @JsonProperty("part")
    @NotNull
    @Schema(description = "Type of the chunk, e.g. firmware, bundle, app. In update server mapped to Software Module Type")
    private String part;

    @JsonProperty("version")
    @NotNull
    @Schema(description = "Software version of the chunk", example = "1.2.0")
    private String version;

    @JsonProperty("name")
    @NotNull
    @Schema(description = "Name of the chunk")
    private String name;

    @JsonProperty("encrypted")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "If encrypted")
    private Boolean encrypted;

    @JsonProperty("artifacts")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "List of artifacts")
    private List<DdiArtifact> artifacts;

    @JsonProperty("metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Meta data of the respective software module that has been marked with 'target visible'")
    private List<DdiMetadata> metadata;

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
    public DdiChunk(
            final String part, final String version, final String name, final Boolean encrypted,
            final List<DdiArtifact> artifacts, final List<DdiMetadata> metadata) {
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