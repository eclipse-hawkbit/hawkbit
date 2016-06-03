/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Tenant specific locally stored artifact representation that is used by
 * {@link SoftwareModule}.
 */
@MappedSuperclass
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public abstract class AbstractJpaArtifact extends AbstractJpaTenantAwareBaseEntity implements Artifact {
    private static final long serialVersionUID = 1L;

    @Column(name = "sha1_hash", length = 40, nullable = true)
    private String sha1Hash;

    @Column(name = "md5_hash", length = 32, nullable = true)
    private String md5Hash;

    @Column(name = "file_size")
    private Long size;

    @Override
    public abstract SoftwareModule getSoftwareModule();

    @Override
    public String getMd5Hash() {
        return md5Hash;
    }

    @Override
    public String getSha1Hash() {
        return sha1Hash;
    }

    public void setMd5Hash(final String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public void setSha1Hash(final String sha1Hash) {
        this.sha1Hash = sha1Hash;
    }

    @Override
    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }
}
