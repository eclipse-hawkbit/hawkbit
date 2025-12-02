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
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Relation element between a {@link DistributionSetType} and its
 * {@link SoftwareModuleType} elements.
 */
@NoArgsConstructor // Default constructor for JPA
@Entity
@Table(name = "sp_ds_type_element")
public class DistributionSetTypeElement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private DistributionSetTypeElementCompositeKey key;

    @MapsId("dsType")
    @ManyToOne(optional = false)
    @JoinColumn(name = "distribution_set_type", nullable = false, updatable = false)
    private JpaDistributionSetType dsType;

    @Getter
    @MapsId("smType")
    @ManyToOne(optional = false)
    @JoinColumn(name = "software_module_type", nullable = false, updatable = false)
    private JpaSoftwareModuleType smType;

    @Setter
    @Getter
    @Column(name = "mandatory")
    private boolean mandatory;

    /**
     * Standard constructor.
     *
     * @param dsType of the element
     * @param smType of the element
     * @param mandatory to <code>true</code> if the {@link SoftwareModuleType} if mandatory element in the {@link DistributionSet}.
     */
    DistributionSetTypeElement(final JpaDistributionSetType dsType, final JpaSoftwareModuleType smType, final boolean mandatory) {
        key = new DistributionSetTypeElementCompositeKey(dsType, smType);
        this.dsType = dsType;
        this.smType = smType;
        this.mandatory = mandatory;
    }

    @Override
    public String toString() {
        return "DistributionSetTypeElement [mandatory=" + mandatory + ", dsType=" + dsType + ", smType=" + smType + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, mandatory);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof DistributionSetTypeElement distributionSetTypeElement &&
                Objects.equals(key, distributionSetTypeElement.key) &&
                Objects.equals(mandatory, distributionSetTypeElement.mandatory);
    }
}