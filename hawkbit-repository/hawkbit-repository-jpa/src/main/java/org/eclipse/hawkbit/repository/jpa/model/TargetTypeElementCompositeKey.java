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
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Composite key for {@link DistributionSetTypeElement}.
 */
@Embeddable
public class TargetTypeElementCompositeKey implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "target_type", nullable = false, updatable = false)
    private Long targetType;

    @Column(name = "distribution_set_type", nullable = false, updatable = false)
    private Long dsType;

    /**
     * Default constructor.
     */
    TargetTypeElementCompositeKey() {
    }

    /**
     * Constructor.
     *
     * @param dsType
     *            in the key
     * @param targetType
     *            in the key
     */
    TargetTypeElementCompositeKey(final JpaTargetType targetType, final JpaDistributionSetType dsType) {
        this.targetType = targetType.getId();
        this.dsType = dsType.getId();
    }

    public Long getDsType() {
        return dsType;
    }

    public void setDsType(final Long dsType) {
        this.dsType = dsType;
    }

    public Long getTargetType() {
        return targetType;
    }

    public void setTargetType(final Long targetType) {
        this.targetType = targetType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dsType == null) ? 0 : dsType.hashCode());
        result = prime * result + ((targetType == null) ? 0 : targetType.hashCode());
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
        final TargetTypeElementCompositeKey other = (TargetTypeElementCompositeKey) obj;
        if (dsType == null) {
            if (other.dsType != null) {
                return false;
            }
        } else if (!dsType.equals(other.dsType)) {
            return false;
        }
        if (targetType == null) {
            if (other.targetType != null) {
                return false;
            }
        } else if (!targetType.equals(other.targetType)) {
            return false;
        }
        return true;
    }

}
