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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation of artifact hash.
 */
public class DmfArtifactHash {

    @JsonProperty
    private String sha1;

    @JsonProperty
    private String md5;

    /**
     * Constructor.
     * 
     * @param sha1
     *            the sha1 hash
     * @param md5
     *            the md5 hash
     */
    @JsonCreator
    public DmfArtifactHash(@JsonProperty("sha1") final String sha1, @JsonProperty("md5") final String md5) {
        this.sha1 = sha1;
        this.md5 = md5;
    }

    public void setSha1(final String sha1) {
        this.sha1 = sha1;
    }

    public void setMd5(final String md5) {
        this.md5 = md5;
    }

    public String getSha1() {
        return sha1;
    }

    public String getMd5() {
        return md5;
    }

}
