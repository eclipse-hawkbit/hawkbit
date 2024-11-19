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
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Relation element between a {@link DistributionSetType} and its
 * {@link SoftwareModuleType} elements.
 */
@Entity
@Table(name = "sp_ds_type_element")
public class DistributionSetTypeElement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private DistributionSetTypeElementCompositeKey key;

    @Column(name = "mandatory")
    private boolean mandatory;

    @MapsId("dsType")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_set_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_type_element_element"))
    private JpaDistributionSetType dsType;

    @MapsId("smType")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "software_module_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_type_element_smtype"))
    private JpaSoftwareModuleType smType;

    public DistributionSetTypeElement() {
        // Default constructor for JPA
    }

    /**
     * Standard constructor.
     *
     * @param dsType of the element
     * @param smType of the element
     * @param mandatory to <code>true</code> if the {@link SoftwareModuleType} if
     *         mandatory element in the {@link DistributionSet}.
     */
    DistributionSetTypeElement(final JpaDistributionSetType dsType, final JpaSoftwareModuleType smType,
            final boolean mandatory) {
        key = new DistributionSetTypeElementCompositeKey(dsType, smType);
        this.dsType = dsType;
        this.smType = smType;
        this.mandatory = mandatory;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    DistributionSetTypeElement setMandatory(final boolean mandatory) {
        this.mandatory = mandatory;
        return this;
    }

    public DistributionSetType getDsType() {
        return dsType;
    }

    public SoftwareModuleType getSmType() {
        return smType;
    }

    public DistributionSetTypeElementCompositeKey getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DistributionSetTypeElement other = (DistributionSetTypeElement) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "DistributionSetTypeElement [mandatory=" + mandatory + ", dsType=" + dsType + ", smType=" + smType + "]";
    }

}
