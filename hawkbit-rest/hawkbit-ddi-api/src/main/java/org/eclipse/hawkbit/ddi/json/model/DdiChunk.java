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

/**
 * Deployment chunks.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiChunk {

    @JsonProperty("part")
    @NotNull
    @Schema(example = "bApp")
    private String part;

    @JsonProperty("version")
    @NotNull
    @Schema(example = "1.2.0")
    private String version;

    @JsonProperty("name")
    @NotNull
    @Schema(example = "oneApp")
    private String name;

    @JsonProperty("encrypted")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean encrypted;

    @JsonProperty("artifacts")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DdiArtifact> artifacts;

    @JsonProperty("metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DdiMetadata> metadata;

    public DdiChunk() {
        // needed for json create
    }

    /**
     * Constructor.
     *
     * @param part
     *            of the deployment chunk
     * @param version
     *            of the artifact
     * @param name
     *            of the artifact
     * @param encrypted
     *            if artifacts are encrypted
     * @param artifacts
     *            download information
     * @param metadata
     *            optional as additional information for the target/device
     */
    public DdiChunk(final String part, final String version, final String name, final Boolean encrypted,
            final List<DdiArtifact> artifacts, final List<DdiMetadata> metadata) {
        this.part = part;
        this.version = version;
        this.name = name;
        this.encrypted = encrypted;
        this.artifacts = artifacts;
        this.metadata = metadata;
    }

    public String getPart() {
        return part;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public Boolean isEncrypted() {
        return encrypted;
    }

    public List<DdiArtifact> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(artifacts);
    }

    public List<DdiMetadata> getMetadata() {
        return metadata;
    }

}
