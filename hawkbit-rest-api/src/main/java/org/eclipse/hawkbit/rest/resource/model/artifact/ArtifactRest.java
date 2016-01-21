/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.artifact;

import org.eclipse.hawkbit.rest.resource.model.BaseEntityRest;
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A json annotated rest model for Artifact to RESTful API representation.
 *
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("Software Artifact")
public class ArtifactRest extends BaseEntityRest {
    @JsonProperty(required = true)
    private ArtifactType type;

    @ApiModelProperty(value = ApiModelProperties.ITEM_ID, required = true)
    @JsonProperty("id")
    private Long artifactId;

    @ApiModelProperty(value = ApiModelProperties.ARTIFACT_HASHES)
    @JsonProperty
    private ArtifactHash hashes;

    @ApiModelProperty(value = ApiModelProperties.ARTIFACT_PROVIDED_FILENAME)
    @JsonProperty
    private String providedFilename;

    @ApiModelProperty(value = ApiModelProperties.ARTIFACT_SIZE)
    @JsonProperty
    private Long size;

    /**
     * @param type
     *            the type to set
     */
    public void setType(final ArtifactType type) {
        this.type = type;
    }

    /**
     * @param hashes
     *            the hashes to set
     */
    @JsonIgnore
    public void setHashes(final ArtifactHash hashes) {
        this.hashes = hashes;
    }

    /**
     * @param artifactId
     *            the artifactId to set
     */
    @JsonIgnore
    public void setArtifactId(final Long artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * @return the type
     */
    public ArtifactType getType() {
        return type;
    }

    /**
     * @return the artifactId
     */
    public Long getArtifactId() {
        return artifactId;
    }

    /**
     * @return the hashes
     */
    public ArtifactHash getHashes() {
        return hashes;
    }

    /**
     * @return the providedFilename
     */
    public String getProvidedFilename() {
        return providedFilename;
    }

    /**
     * @param providedFilename
     *            the providedFilename to set
     */
    @JsonIgnore
    public void setProvidedFilename(final String providedFilename) {
        this.providedFilename = providedFilename;
    }

    /**
     * Type maps to either {@link LocalArtifact} or {@link ExternalArtifact}.
     *
     *
     *
     *
     */
    public enum ArtifactType {
        LOCAL("local"), EXTERNAL("external");

        private final String name;

        private ArtifactType(final String name) {
            this.name = name;
        }

        /**
         * @return the name
         */
        @JsonValue
        public String getName() {
            return name;
        }
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
    @JsonIgnore
    public void setSize(final Long size) {
        this.size = size;
    }

}
