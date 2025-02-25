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
import lombok.Data;

/**
 * JSON representation of artifact hash.
 */
@Data
public class DmfArtifactHash {

    private final String sha1;
    private final String md5;

    /**
     * Constructor.
     *
     * @param sha1 the sha1 hash
     * @param md5 the md5 hash
     */
    @JsonCreator
    public DmfArtifactHash(@JsonProperty("sha1") final String sha1, @JsonProperty("md5") final String md5) {
        this.sha1 = sha1;
        this.md5 = md5;
    }
}