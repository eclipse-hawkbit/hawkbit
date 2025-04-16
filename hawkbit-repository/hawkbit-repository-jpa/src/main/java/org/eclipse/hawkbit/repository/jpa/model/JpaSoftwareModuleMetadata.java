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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;

/**
 * Metadata for {@link SoftwareModule}.
 */
@NoArgsConstructor // Default constructor for JPA
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@IdClass(SwMetadataCompositeKey.class)
@Entity
@Table(name = "sp_sm_metadata")
public class JpaSoftwareModuleMetadata extends AbstractJpaMetaData implements SoftwareModuleMetadata {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sw_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_metadata_sw"))
    private JpaSoftwareModule softwareModule;

    @Column(name = "target_visible")
    private boolean targetVisible;

    public JpaSoftwareModuleMetadata(final String key, final SoftwareModule softwareModule, final String value) {
        super(key, value);
        this.softwareModule = (JpaSoftwareModule) softwareModule;
    }

    public JpaSoftwareModuleMetadata(final String key, final SoftwareModule softwareModule, final String value, final boolean targetVisible) {
        super(key, value);
        this.softwareModule = (JpaSoftwareModule) softwareModule;
        this.targetVisible = targetVisible;
    }

    public SwMetadataCompositeKey getId() {
        return new SwMetadataCompositeKey(softwareModule.getId(), getKey());
    }
}