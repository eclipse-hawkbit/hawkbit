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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Hashes for given Artifact
 */
@NoArgsConstructor // needed for json create
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiArtifactHash {

    @JsonProperty
    @Schema(description = "SHA1 hash of the artifact in Base 16 format", example = "2d86c2a659e364e9abba49ea6ffcd53dd5559f05")
    private String sha1;

    @JsonProperty
    @Schema(description = "MD5 hash of the artifact", example = "0d1b08c34858921bc7c662b228acb7ba")
    private String md5;

    @JsonProperty
    @JsonInclude(Include.NON_NULL)
    @Schema(description = "SHA-256 hash of the artifact in Base 16 format", example = "a03b221c6c6eae7122ca51695d456d5222e524889136394944b2f9763b483615")
    private String sha256;

    /**
     * Public constructor.
     *
     * @param sha1
     *         sha1 hash of the artifact
     * @param md5
     *         md5 hash of the artifact
     * @param sha256
     *         sha256 hash of the artifact
     */
    public DdiArtifactHash(final String sha1, final String md5, final String sha256) {
        this.sha1 = sha1;
        this.md5 = md5;
        this.sha256 = sha256;
    }
}