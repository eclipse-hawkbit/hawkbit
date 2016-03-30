/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Tenant specific locally stored artifact representation that is used by
 * {@link SoftwareModule}.
 */
@MappedSuperclass
public abstract class Artifact extends TenantAwareBaseEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "sha1_hash", length = 40, nullable = true)
    private String sha1Hash;

    @Column(name = "md5_hash", length = 32, nullable = true)
    private String md5Hash;

    @Column(name = "file_size")
    private Long size;

    public abstract SoftwareModule getSoftwareModule();

    public String getMd5Hash() {
        return md5Hash;
    }

    public String getSha1Hash() {
        return sha1Hash;
    }

    public void setMd5Hash(final String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public void setSha1Hash(final String sha1Hash) {
        this.sha1Hash = sha1Hash;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Artifact)) {
            return false;
        }

        return true;
    }
}
