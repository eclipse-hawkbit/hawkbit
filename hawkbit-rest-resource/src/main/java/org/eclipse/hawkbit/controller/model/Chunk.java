/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Deployment chunks.
 *
 */
public class Chunk {

    @NotNull
    private final String part;

    @NotNull
    private final String version;

    @NotNull
    private final String name;

    private final List<Artifact> artifacts;

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
     *
     */
    public Chunk(final String part, final String version, final String name, final List<Artifact> artifacts) {
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

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the artifacts
     */
    public List<Artifact> getArtifacts() {
        return artifacts;
    }

}
