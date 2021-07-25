/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.persistence.annotations.CascadeOnDelete;

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
import java.io.Serializable;

/**
 * Relation element between a {@link TargetType} and its
 * {@link DistributionSetType} elements.
 *
 */
@Entity
@Table(name = "sp_target_type_element")
public class TargetTypeElement implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private TargetTypeElementCompositeKey key;

    @CascadeOnDelete
    @MapsId("targetType")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_type_element_element"))
    private JpaTargetType targetType;

    @CascadeOnDelete
    @MapsId("dsType")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_set_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_type_element_dstype"))
    private JpaDistributionSetType dsType;

    public TargetTypeElement() {
        // Default constructor for JPA
    }

    /**
     * Standard constructor.
     *
     * @param dsType
     *            of the element
     * @param targetType
     *            of the element
     */
    TargetTypeElement(final JpaTargetType targetType, final JpaDistributionSetType dsType) {
        key = new TargetTypeElementCompositeKey(targetType, dsType);
        this.dsType = dsType;
        this.targetType = targetType;
    }

    public DistributionSetType getDsType() {
        return dsType;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public TargetTypeElementCompositeKey getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "DistributionSetTypeElement [dsType=" + dsType + ", targetType=" + targetType + "]";
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
        final TargetTypeElement other = (TargetTypeElement) obj;
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
