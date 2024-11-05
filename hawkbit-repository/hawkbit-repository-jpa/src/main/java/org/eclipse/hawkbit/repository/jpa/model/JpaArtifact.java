/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * JPA implementation of {@link Artifact}.
 */
@Table(name = "sp_artifact", indexes = { @Index(name = "sp_idx_artifact_01", columnList = "tenant,software_module"),
        @Index(name = "sp_idx_artifact_02", columnList = "tenant,sha1_hash"),
        @Index(name = "sp_idx_artifact_prim", columnList = "tenant,id") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaArtifact extends AbstractJpaTenantAwareBaseEntity implements Artifact {

    private static final long serialVersionUID = 1L;

    @Column(name = "sha1_hash", length = 40, nullable = false, updatable = false)
    @Size(min = 1, max = 40)
    @NotNull
    private String sha1Hash;

    @Column(name = "provided_file_name", length = 256, updatable = false)
    @Size(min = 1, max = 256)
    @NotNull
    private String filename;

    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST }, fetch = FetchType.LAZY)
    @JoinColumn(name = "software_module", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_assigned_sm"))
    private JpaSoftwareModule softwareModule;

    @Column(name = "md5_hash", length = 32, updatable = false, nullable = true)
    private String md5Hash;

    @Column(name = "sha256_hash", length = 64, updatable = false, nullable = true)
    private String sha256Hash;

    @Column(name = "file_size", updatable = false)
    private long size;

    /**
     * Default constructor needed for JPA entities..
     */
    public JpaArtifact() {
        // Default constructor needed for JPA entities.
    }

    /**
     * Constructs artifact.
     *
     * @param sha1Hash that is the link to the {@link AbstractDbArtifact} entity.
     * @param filename that is used by {@link AbstractDbArtifact} store.
     * @param softwareModule of this artifact
     */
    public JpaArtifact(@NotEmpty final String sha1Hash, @NotNull final String filename,
            final SoftwareModule softwareModule) {
        this.sha1Hash = sha1Hash;
        this.filename = filename;
        this.softwareModule = (JpaSoftwareModule) softwareModule;
        this.softwareModule.addArtifact(this);
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    @Override
    public String getMd5Hash() {
        return md5Hash;
    }

    @Override
    public String getSha1Hash() {
        return sha1Hash;
    }

    @Override
    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(final String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    @Override
    public long getSize() {
        return size;
    }

    public void setSize(final long size) {
        this.size = size;
    }

    public void setSha1Hash(final String sha1Hash) {
        this.sha1Hash = sha1Hash;
    }

    public void setMd5Hash(final String md5Hash) {
        this.md5Hash = md5Hash;
    }
}
