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
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Download information for all artifacts related to a specific {@link Chunk}.
 *
 *
 *
 *
 */
@ApiModel(ApiModelProperties.ARTIFACTS)
public class Artifact extends ResourceSupport {

    @ApiModelProperty(value = ApiModelProperties.ARTIFACT_PROVIDED_FILENAME, required = true)
    @NotNull
    @JsonProperty
    private String filename;

    @ApiModelProperty(value = ApiModelProperties.ARTIFACT_HASHES)
    @JsonProperty
    private ArtifactHash hashes;

    @ApiModelProperty(value = ApiModelProperties.ARTIFACT_SIZE)
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
