/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JSON representation of artifact.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfArtifact {

    private final String filename;
    private final DmfArtifactHash hashes;
    private final long size;
    private final long lastModified;
    private final Map<String, String> urls;

    @JsonCreator
    public DmfArtifact(
            @JsonProperty("filename") final String filename,
            @JsonProperty("hashes") final DmfArtifactHash hashes,
            @JsonProperty("size") final long size,
            @JsonProperty("lastModified") final long lastModified,
            @JsonProperty("urls") final Map<String, String> urls) {
        this.filename = filename;
        this.hashes = hashes;
        this.size = size;
        this.lastModified = lastModified;
        this.urls = urls == null ? Collections.emptyMap() : Collections.unmodifiableMap(urls);
    }
}