/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Hashes for given Artifact.
 *
 *
 */
public class DdiArtifactHash {

    @JsonProperty
    private String sha1;

    @JsonProperty
    private String md5;

    @JsonProperty
    private String sha256;

    /**
     * Default constructor.
     */
    public DdiArtifactHash() {
        // needed for json create
    }

    /**
     * Public constructor.
     *
     * @param sha1
     * @param md5
     */
    public DdiArtifactHash(final String sha1, final String md5, final String sha256) {
        this.sha1 = sha1;
        this.md5 = md5;
        this.sha256 = sha256;
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

    /**
     * @return the sh256
     */
    public String getSha256() {
        return sha256;
    }
}
