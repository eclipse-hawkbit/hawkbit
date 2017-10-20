/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.model;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON annotated REST model to hold the artifact data for
 * {@link SoftwareModuleInfo}. An artifact represents a file that has been
 * installed or updated.
 */
@JsonInclude(Include.NON_NULL)
public class Artifact {

    @JsonProperty(value = "filename", required = true)
    private String filename;

    @JsonProperty(value = "version", required = true)
    private String version;

    @JsonProperty(value = "href")
    private String href;

    @JsonProperty(value = "md5Hash")
    private String md5Hash;

    @JsonProperty(value = "sha1Hash")
    private String sha1Hash;

    @JsonIgnore
    private MultipartFile file;

    /**
     * Returns the file represented by {@link Artifact} as
     * {@link MultipartFile}.
     *
     * @return file
     */
    @JsonIgnore
    public MultipartFile getFile() {
        return file;
    }

    /**
     * Sets the file that will be represented by this {@link Artifact}.
     *
     * @param file.
     */
    @JsonIgnore
    public void setFile(MultipartFile file) {
        this.file = file;
    }

    /**
     * Returns the md5Hash for the file represented by this {@link Artifact}.
     *
     * @return md5Hash.
     */
    public String getMd5Hash() {
        return md5Hash;
    }

    /**
     * Sets the md5Hash for the file represented by this {@link Artifact}.
     *
     * @param md5Hash
     */
    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    /**
     * Returns the sha1Hash for the file represented by this {@link Artifact}.
     *
     * @return sha1Hash.
     */
    public String getSha1Hash() {
        return sha1Hash;
    }

    /**
     * Sets the sha1Hash for the file represented by this {@link Artifact}.
     *
     * @param sha1Hash.
     */
    public void setSha1Hash(String sha1Hash) {
        this.sha1Hash = sha1Hash;
    }

    /**
     * Returns the filename of this {@link Artifact}.
     *
     * @return filename.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the filename of this {@link Artifact}.
     *
     * @param filename.
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Returns the version of the {@link Artifact}.
     *
     * @return version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the {@link Artifact}.
     *
     * @param version.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the href of the {@link Artifact}.
     *
     * @return href.
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the href of the {@link Artifact}.
     *
     * @param href.
     */
    public void setHref(String href) {
        this.href = href;
    }
}
