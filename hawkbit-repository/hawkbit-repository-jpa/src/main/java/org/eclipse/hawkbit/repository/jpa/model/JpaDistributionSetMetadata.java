/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;

/**
 * Meta data for {@link DistributionSet}.
 *
 */
@IdClass(DsMetadataCompositeKey.class)
@Entity
@Table(name = "sp_ds_metadata")
public class JpaDistributionSetMetadata extends AbstractJpaMetaData implements DistributionSetMetadata {
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ds_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_metadata_ds"))
    private JpaDistributionSet distributionSet;

    public JpaDistributionSetMetadata() {
        // default public constructor for JPA
    }

    public JpaDistributionSetMetadata(final String key, final String value) {
        super(key, value);
    }

    public JpaDistributionSetMetadata(final String key, final DistributionSet distributionSet, final String value) {
        super(key, value);
        this.distributionSet = (JpaDistributionSet) distributionSet;
    }

    public DsMetadataCompositeKey getId() {
        return new DsMetadataCompositeKey(distributionSet.getId(), getKey());
    }

    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = (JpaDistributionSet) distributionSet;
    }

    @Override
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((distributionSet == null) ? 0 : distributionSet.hashCode());
        return result;
    }

    @Override
    // exception squid:S2259 - obj is checked for null in super
    @SuppressWarnings("squid:S2259")
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final JpaDistributionSetMetadata other = (JpaDistributionSetMetadata) obj;
        if (distributionSet == null) {
            if (other.distributionSet != null) {
                return false;
            }
        } else if (!distributionSet.equals(other.distributionSet)) {
            return false;
        }
        return true;
    }
}
