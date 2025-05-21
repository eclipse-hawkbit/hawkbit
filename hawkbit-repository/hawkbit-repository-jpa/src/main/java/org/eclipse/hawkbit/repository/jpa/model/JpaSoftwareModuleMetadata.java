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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;

/**
 * Metadata for {@link SoftwareModule}.
 */
@NoArgsConstructor // Default constructor for JPA
@Data
@IdClass(SwMetadataCompositeKey.class)
@Entity
@Table(name = "sp_sm_metadata")
public class JpaSoftwareModuleMetadata implements SoftwareModuleMetadata {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sm", nullable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_sm_metadata_sm"))
    private JpaSoftwareModule softwareModule;

    @Id
    @Column(name = "meta_key", nullable = false, length = SoftwareModule.METADATA_KEY_MAX_SIZE, updatable = false)
    @Size(min = 1, max = SoftwareModule.METADATA_KEY_MAX_SIZE)
    @NotNull
    private String key;

    @Column(name = "meta_value", length = SoftwareModule.METADATA_VALUE_MAX_SIZE)
    @Size(max = SoftwareModule.METADATA_VALUE_MAX_SIZE)
    @Basic
    private String value;

    @Column(name = "target_visible")
    private boolean targetVisible;

    public JpaSoftwareModuleMetadata(final String key, final SoftwareModule softwareModule, final String value) {
        this(key, softwareModule, value, false);
    }

    public JpaSoftwareModuleMetadata(final String key, final SoftwareModule softwareModule, final String value, final boolean targetVisible) {
        this.key = key;
        this.value = value;
        this.softwareModule = (JpaSoftwareModule) softwareModule;
        this.targetVisible = targetVisible;
    }

    public SwMetadataCompositeKey getId() {
        return new SwMetadataCompositeKey(softwareModule.getId(), getKey());
    }
}