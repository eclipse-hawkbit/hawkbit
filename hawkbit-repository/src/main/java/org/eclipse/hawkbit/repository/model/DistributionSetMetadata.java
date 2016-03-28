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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Metadata for {@link DistributionSet}.
 *
 *
 *
 *
 */
@IdClass(DsMetadataCompositeKey.class)
@Entity
@Table(name = "sp_ds_metadata")
public class DistributionSetMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "meta_key", length = 128)
    private String key;

    @Column(name = "meta_value", length = 4000)
    @Basic
    private String value;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ds_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_metadata_ds"))
    private DistributionSet distributionSet;

    public DistributionSetMetadata() {
        // Default constructor for JPA.
    }

    /**
     * Parameter constructor.
     *
     * @param key
     * @param distributionSet
     * @param value
     */
    public DistributionSetMetadata(final String key, final DistributionSet distributionSet, final String value) {
        this.key = key;
        this.distributionSet = distributionSet;
        this.value = value;
    }

    public DsMetadataCompositeKey getId() {
        return new DsMetadataCompositeKey(distributionSet, key);
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = distributionSet;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (distributionSet == null ? 0 : distributionSet.hashCode());
        result = prime * result + (key == null ? 0 : key.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
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
        if (!(obj instanceof DistributionSetMetadata)) {
            return false;
        }
        final DistributionSetMetadata other = (DistributionSetMetadata) obj;
        if (distributionSet == null) {
            if (other.distributionSet != null) {
                return false;
            }
        } else if (!distributionSet.equals(other.distributionSet)) {
            return false;
        }
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

}
