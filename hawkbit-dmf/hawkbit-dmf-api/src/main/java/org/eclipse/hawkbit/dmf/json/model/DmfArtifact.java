/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation of artifact.
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfArtifact {
    @JsonProperty
    private String filename;

    @JsonProperty
    private DmfArtifactHash hashes;

    @JsonProperty
    private Long size;

    @JsonProperty
    private Map<String, String> urls;

    public Map<String, String> getUrls() {
        if (urls == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(urls);
    }

    public void setUrls(final Map<String, String> urls) {
        this.urls = urls;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public DmfArtifactHash getHashes() {
        return hashes;
    }

    public void setHashes(final DmfArtifactHash hashes) {
        this.hashes = hashes;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

}
