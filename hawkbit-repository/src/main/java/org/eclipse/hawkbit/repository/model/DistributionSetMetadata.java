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

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "meta_key", length = 128)
    private String key;

    @Column(name = "meta_value", length = 4000)
    @Basic
    private String value;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ds_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_metadata_ds") )
    private DistributionSet distributionSet;

    public DistributionSetMetadata() {

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

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * @param distributionSet
     *            the distributionSet to set
     */
    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = distributionSet;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * @return the distributionSet
     */
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

}
