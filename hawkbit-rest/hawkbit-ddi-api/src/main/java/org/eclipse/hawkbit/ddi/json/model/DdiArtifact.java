/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Download information for all artifacts related to a specific {@link DdiChunk}
 * .
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiArtifact extends RepresentationModel<DdiArtifact> {

    @NotNull
    @JsonProperty
    @Schema(example = "binary.tgz")
    private String filename;

    @JsonProperty
    private DdiArtifactHash hashes;

    @JsonProperty
    @Schema(example = "3")
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
