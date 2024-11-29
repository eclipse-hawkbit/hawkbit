/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;

/**
 * Meta data for {@link Target}.
 */
@NoArgsConstructor(access = AccessLevel.PUBLIC) // Default constructor for JPA
@Setter
@Getter
@IdClass(TargetMetadataCompositeKey.class)
@Entity
@Table(name = "sp_target_metadata")
public class JpaTargetMetadata extends AbstractJpaMetaData implements TargetMetadata {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_metadata_target"))
    private JpaTarget target;

    /**
     * Creates a single metadata entry with the given key and value.
     *
     * @param key of the meta-data entry
     * @param value of the meta-data entry
     */
    public JpaTargetMetadata(final String key, final String value) {
        super(key, value);
    }

    /**
     * Creates a single metadata entry with the given key and value for the
     * given {@link Target}.
     *
     * @param key of the meta-data entry
     * @param value of the meta-data entry
     * @param target the meta-data entry is associated with
     */
    public JpaTargetMetadata(final String key, final String value, final Target target) {
        super(key, value);
        this.target = (JpaTarget) target;
    }

    public TargetMetadataCompositeKey getId() {
        return new TargetMetadataCompositeKey(target.getId(), getKey());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        return result;
    }

    @Override
    // exception squid:S2259 - obj is checked for null in super
    @SuppressWarnings("squid:S2259")
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final JpaTargetMetadata other = (JpaTargetMetadata) obj;
        if (target == null) {
            return other.target == null;
        } else {
            return target.equals(other.target);
        }
    }
}
