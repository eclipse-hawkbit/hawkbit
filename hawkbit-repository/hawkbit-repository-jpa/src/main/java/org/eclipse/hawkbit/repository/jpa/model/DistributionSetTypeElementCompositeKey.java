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
import jakarta.persistence.Embeddable;

/**
 * Composite key for {@link DistributionSetTypeElement}.
 */
@Embeddable
public class DistributionSetTypeElementCompositeKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "distribution_set_type", nullable = false, updatable = false)
    private Long dsType;

    @Column(name = "software_module_type", nullable = false, updatable = false)
    private Long smType;

    /**
     * Default constructor.
     */
    DistributionSetTypeElementCompositeKey() {
    }

    /**
     * Constructor.
     *
     * @param dsType in the key
     * @param smType in the key
     */
    DistributionSetTypeElementCompositeKey(final JpaDistributionSetType dsType, final JpaSoftwareModuleType smType) {
        this.dsType = dsType.getId();
        this.smType = smType.getId();
    }

    public Long getDsType() {
        return dsType;
    }

    public void setDsType(final Long dsType) {
        this.dsType = dsType;
    }

    public Long getSmType() {
        return smType;
    }

    public void setSmType(final Long smType) {
        this.smType = smType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dsType == null) ? 0 : dsType.hashCode());
        result = prime * result + ((smType == null) ? 0 : smType.hashCode());
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
        final DistributionSetTypeElementCompositeKey other = (DistributionSetTypeElementCompositeKey) obj;
        if (dsType == null) {
            if (other.dsType != null) {
                return false;
            }
        } else if (!dsType.equals(other.dsType)) {
            return false;
        }
        if (smType == null) {
            if (other.smType != null) {
                return false;
            }
        } else if (!smType.equals(other.smType)) {
            return false;
        }
        return true;
    }

}
