/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.artifact;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Hashes for given Artifact.
 *
 *
 *
 *
 */
@ApiModel(ApiModelProperties.ARTIFACT_HASHES)
public class ArtifactHash {

    @ApiModelProperty(value = ApiModelProperties.ARTIFACT_HASHES_SHA1)
    @JsonProperty
    private String sha1;

    @ApiModelProperty(value = ApiModelProperties.ARTIFACT_HASHES_MD5)
    @JsonProperty
    private String md5;

    /**
     * Default constructor.
     */
    public ArtifactHash() {
    }

    /**
     * Public constructor.
     *
     * @param sha1
     * @param md5
     */
    public ArtifactHash(final String sha1, final String md5) {
        super();
        this.sha1 = sha1;
        this.md5 = md5;
    }

    /**
     * @return the sha1
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * @return the md5
     */
    public String getMd5() {
        return md5;
    }

}
