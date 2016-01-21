/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.rest.resource.model.artifact.ArtifactHash;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Download information for all artifacts related to a specific {@link Chunk}.
 *
 */
public class Artifact extends ResourceSupport {

    @NotNull
    @JsonProperty
    private String filename;

    @JsonProperty
    private ArtifactHash hashes;

    @JsonProperty
    private Long size;

    /**
     * @return the hashes
     */
    public ArtifactHash getHashes() {
        return hashes;
    }

    /**
     * @param hashes
     *            the hashes to set
     */
    public void setHashes(final ArtifactHash hashes) {
        this.hashes = hashes;
    }

    /**
     * @return the fileName
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param fileName
     *            the fileName to set
     */
    public void setFilename(final String fileName) {
        filename = fileName;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @param size
     *            the size to set
     */
    public void setSize(final Long size) {
        this.size = size;
    }

}
