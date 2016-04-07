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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Metadata for {@link SoftwareModule}.
 *
 */
@IdClass(SwMetadataCompositeKey.class)
@Entity
@Table(name = "sp_sw_metadata")
public class SoftwareModuleMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "meta_key", length = 128)
    private String key;

    @Column(name = "meta_value", length = 4000)
    private String value;

    @Id
    @ManyToOne(targetEntity = SoftwareModule.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "sw_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_metadata_sw"))
    private SoftwareModule softwareModule;

    /**
     * Default constructor for JPA.
     */
    public SoftwareModuleMetadata() {
        // default constructor for JPA.
    }

    /**
     * Standard constructor.
     *
     * @param key
     *            of the meta data element
     * @param softwareModule
     * @param value
     *            of the meta data element
     */
    public SoftwareModuleMetadata(final String key, final SoftwareModule softwareModule, final String value) {
        this.key = key;
        this.softwareModule = softwareModule;
        this.value = value;
    }

    public SwMetadataCompositeKey getId() {
        return new SwMetadataCompositeKey(softwareModule, key);
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    public void setSoftwareModule(final SoftwareModule softwareModule) {
        this.softwareModule = softwareModule;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "SoftwareModuleMetadata [key=" + key + ", value=" + value + ", softwareModule=" + softwareModule + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((softwareModule == null) ? 0 : softwareModule.hashCode());
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
        if (!(obj instanceof SoftwareModuleMetadata)) {
            return false;
        }
        final SoftwareModuleMetadata other = (SoftwareModuleMetadata) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (softwareModule == null) {
            if (other.softwareModule != null) {
                return false;
            }
        } else if (!softwareModule.equals(other.softwareModule)) {
            return false;
        }
        return true;
    }

}
