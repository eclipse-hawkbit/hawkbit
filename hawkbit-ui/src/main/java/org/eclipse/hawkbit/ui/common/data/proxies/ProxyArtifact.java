/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.model.Artifact;

/**
 * Proxy for {@link Artifact}
 */
public class ProxyArtifact extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    private String filename;
    private String md5Hash;
    private String sha1Hash;
    private String sha256Hash;
    private long size;
    private String modifiedDate;

    /**
     * Gets the name of the file
     *
     * @return filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the filename
     *
     * @param filename
     *          Name of the file
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * Gets the md5 format hash value
     *
     * @return md5Hash
     */
    public String getMd5Hash() {
        return md5Hash;
    }

    /**
     * Sets the md5Hash
     *
     * @param md5Hash
     *          md5 format hash value
     */
    public void setMd5Hash(final String md5Hash) {
        this.md5Hash = md5Hash;
    }

    /**
     * Gets the sha1 format hash value
     *
     * @return sha1Hash
     */
    public String getSha1Hash() {
        return sha1Hash;
    }

    /**
     * Sets the sha1Hash
     *
     * @param sha1Hash
     *          sha1 format hash value
     */
    public void setSha1Hash(final String sha1Hash) {
        this.sha1Hash = sha1Hash;
    }

    /**
     * Gets the size of file
     *
     * @return file size
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the file size
     *
     * @param size
     *          size of file
     */
    public void setSize(final long size) {
        this.size = size;
    }

    /**
     * Gets the sha256 format hash value
     *
     * @return sha256Hash
     */
    public String getSha256Hash() {
        return sha256Hash;
    }

    /**
     * Sets the sha256Hash
     *
     * @param sha256Hash
     *          sha256 format hash value
     */
    public void setSha256Hash(final String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    /**
     * Gets the artifact modified date
     *
     * @return modifiedDate
     */
    public String getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Sets the modifiedDate
     *
     * @param modifiedDate
     *          artifact modified date
     */
    public void setModifiedDate(final String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

}
