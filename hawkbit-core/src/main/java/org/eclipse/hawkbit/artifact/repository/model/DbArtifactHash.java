/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository.model;

/**
 * Database representation of artifact hash.
 * 
 */
public class DbArtifactHash {

    private final String sha1;

    private final String md5;

    private final String sha256;

    /**
     * Constructor.
     * 
     * @param sha1
     *            the sha1 hash
     * @param md5
     *            the md5 hash
     * @param sha256
     *            the sha256 hash
     */
    public DbArtifactHash(final String sha1, final String md5, final String sha256) {
        this.sha1 = sha1;
        this.md5 = md5;
        this.sha256 = sha256;
    }

    public String getSha1() {
        return sha1;
    }

    public String getMd5() {
        return md5;
    }

    public String getSha256() {
        return sha256;
    }
}
