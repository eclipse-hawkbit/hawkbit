/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.artifact;

import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A json annotated rest model for Artifact to RESTful API representation.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(value = "")
public class MgmtArtifact extends MgmtBaseEntity {

    @JsonProperty(required = true)
    private ArtifactType type;

    @JsonProperty("id")
    private Long artifactId;

    @JsonProperty
    private MgmtArtifactHash hashes;

    @JsonProperty
    private String providedFilename;

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
    public void setHashes(final MgmtArtifactHash hashes) {
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
    public MgmtArtifactHash getHashes() {
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
