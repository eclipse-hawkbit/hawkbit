/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import javax.validation.constraints.NotNull;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Download information for all artifacts related to a specific {@link DdiChunk}
 * .
 */
public class DdiArtifact extends ResourceSupport {

    @NotNull
    @JsonProperty
    private String filename;

    @JsonProperty
    private DdiArtifactHash hashes;

    @JsonProperty
    private Long size;

    public DdiArtifactHash getHashes() {
        return hashes;
    }

    public void setHashes(final DdiArtifactHash hashes) {
        this.hashes = hashes;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String fileName) {
        filename = fileName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

}
