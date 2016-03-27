/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

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
    @JoinColumn(name = "distribution_set_type", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_type_element_dstype"))
    private DistributionSetType dsType;

    @MapsId("smType")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "software_module_type", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_type_element_smtype"))
    private SoftwareModuleType smType;

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
    public DistributionSetTypeElement(final DistributionSetType dsType, final SoftwareModuleType smType,
            final boolean mandatory) {
        super();
        this.key = new DistributionSetTypeElementCompositeKey(dsType, smType);
        this.dsType = dsType;
        this.smType = smType;
        this.mandatory = mandatory;
    }

    /**
     * @return the mandatory
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * @return the dsType
     */
    public DistributionSetType getDsType() {
        return dsType;
    }

    /**
     * @return the smType
     */
    public SoftwareModuleType getSmType() {
        return smType;
    }

    /**
     * @return the key
     */
    public DistributionSetTypeElementCompositeKey getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + (mandatory ? 1231 : 1237);
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
        if (!(obj instanceof DistributionSetTypeElement)) {
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
        if (mandatory != other.mandatory) {
            return false;
        }
        return true;
    }

}
