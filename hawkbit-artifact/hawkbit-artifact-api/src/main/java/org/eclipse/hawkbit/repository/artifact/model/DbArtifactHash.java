/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.artifact.model;

import lombok.Data;

/**
 * Database representation of artifact hash.
 */
@Data
public class DbArtifactHash {

    private final String sha1;
    private final String md5;
    private final String sha256;

    /**
     * Constructor.
     *
     * @param sha1 the sha1 hash
     * @param md5 the md5 hash
     * @param sha256 the sha256 hash
     */
    public DbArtifactHash(final String sha1, final String md5, final String sha256) {
        this.sha1 = sha1;
        this.md5 = md5;
        this.sha256 = sha256;
    }
}