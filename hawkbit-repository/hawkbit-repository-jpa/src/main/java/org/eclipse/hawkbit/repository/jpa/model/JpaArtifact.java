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

import java.io.Serial;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * JPA implementation of {@link Artifact}.
 */
@NoArgsConstructor // Default constructor needed for JPA entities.
@Setter
@Getter
@Table(name = "sp_artifact")
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaArtifact extends AbstractJpaTenantAwareBaseEntity implements Artifact {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST }, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "software_module", nullable = false, updatable = false)
    private JpaSoftwareModule softwareModule;

    @Column(name = "provided_file_name", length = 256, updatable = false)
    @Size(min = 1, max = 256)
    @NotNull
    private String filename;

    @Column(name = "md5_hash", length = 32, updatable = false)
    private String md5Hash;

    @Column(name = "sha1_hash", length = 40, nullable = false, updatable = false)
    @Size(min = 1, max = 40)
    @NotNull
    private String sha1Hash;

    @Column(name = "sha256_hash", length = 64, updatable = false)
    private String sha256Hash;

    @Column(name = "file_size", updatable = false)
    private long fileSize;

    public JpaArtifact(@NotEmpty final String sha1Hash, @NotNull final String filename, final SoftwareModule softwareModule) {
        this.sha1Hash = sha1Hash;
        this.filename = filename;
        this.softwareModule = (JpaSoftwareModule) softwareModule;
    }

    @Override
    public long getSize() {
        return getFileSize();
    }
}