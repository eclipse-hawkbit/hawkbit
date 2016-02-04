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
 *
 *
 *
 */
@IdClass(SwMetadataCompositeKey.class)
@Entity
@Table(name = "sp_sw_metadata")
public class SoftwareModuleMetadata implements Serializable {

    /**
    *
    */
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

    public SoftwareModuleMetadata() {

    }

    /**
     * Standard constructor.
     *
     * @param key
     *            of the metadata element
     * @param softwareModule
     * @param value
     *            of the metadata element
     */
    public SoftwareModuleMetadata(final String key, final SoftwareModule softwareModule, final String value) {
        this.key = key;
        this.softwareModule = softwareModule;
        this.value = value;
    }

    /**
     * @return the id
     */
    public SwMetadataCompositeKey getId() {
        return new SwMetadataCompositeKey(softwareModule, key);
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
     * @return the softwareModule
     */
    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    /**
     * @param softwareModule
     *            the softwareModule to set
     */
    public void setSoftwareModule(final SoftwareModule softwareModule) {
        this.softwareModule = softwareModule;
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

}
