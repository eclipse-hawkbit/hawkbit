/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Relation element between a {@link DistributionSetType} and its
 * {@link SoftwareModuleType} elements.
 *
 */
@Entity
@Table(name = "sp_ds_type_element")
public class DistributionSetTypeElement implements Serializable {
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
     * @param dsType
     *            of the element
     * @param smType
     *            of the element
     * @param mandatory
     *            to <code>true</code> if the {@link SoftwareModuleType} if
     *            mandatory element in the {@link DistributionSet}.
     */
    DistributionSetTypeElement(final JpaDistributionSetType dsType, final JpaSoftwareModuleType smType,
            final boolean mandatory) {
        super();
        key = new DistributionSetTypeElementCompositeKey(dsType, smType);
        this.dsType = dsType;
        this.smType = smType;
        this.mandatory = mandatory;
    }

    DistributionSetTypeElement setMandatory(final boolean mandatory) {
        this.mandatory = mandatory;
        return this;
    }

    public boolean isMandatory() {
        return mandatory;
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
    public String toString() {
        return "DistributionSetTypeElement [mandatory=" + mandatory + ", dsType=" + dsType + ", smType=" + smType + "]";
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

}
