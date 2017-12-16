/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Deployment chunks.
 */
public class DdiChunk {

    @JsonProperty("part")
    @NotNull
    private String part;

    @JsonProperty("version")
    @NotNull
    private String version;

    @JsonProperty("name")
    @NotNull
    private String name;

    @JsonProperty("artifacts")
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
     * @param artifacts
     *            download information
     * @param metadata
     *            optional as additional information for the target/device
     */
    public DdiChunk(final String part, final String version, final String name, final List<DdiArtifact> artifacts,
            final List<DdiMetadata> metadata) {
        this.part = part;
        this.version = version;
        this.name = name;
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
