/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.artifact;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Artifact to RESTful API representation.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtArtifact extends MgmtBaseEntity {

    @JsonProperty("id")
    @Schema(example = "3")
    private Long artifactId;

    @JsonProperty
    private MgmtArtifactHash hashes;

    @JsonProperty
    @Schema(example = "file1")
    private String providedFilename;

    @JsonProperty
    @Schema(example = "3")
    private Long size;

    public MgmtArtifact() {
        // need for json encoder
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
