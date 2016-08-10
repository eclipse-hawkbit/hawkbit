/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Deployment chunks.
 */
public class DdiChunk {

    @NotNull
    private String part;

    @NotNull
    private String version;

    @NotNull
    private String name;

    private List<DdiArtifact> artifacts;

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
     */
    public DdiChunk(final String part, final String version, final String name, final List<DdiArtifact> artifacts) {
        super();
        this.part = part;
        this.version = version;
        this.name = name;
        this.artifacts = artifacts;
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
        return artifacts;
    }

}
