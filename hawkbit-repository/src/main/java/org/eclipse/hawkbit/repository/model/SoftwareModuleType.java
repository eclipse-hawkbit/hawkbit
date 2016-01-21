/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Type of a software modules.
 *
 *
 *
 *
 */
@Entity
@Table(name = "sp_software_module_type", indexes = {
        @Index(name = "sp_idx_software_module_type_01", columnList = "tenant,deleted"),
        @Index(name = "sp_idx_software_module_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "type_key", "tenant" }, name = "uk_smt_type_key"),
                @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_smt_name") })
public class SoftwareModuleType extends NamedEntity {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Column(name = "type_key", nullable = false, length = 64)
    private String key;

    @Column(name = "max_ds_assignments", nullable = false)
    private int maxAssignments;

    @Column(name = "colour", nullable = true, length = 16)
    private String colour;

    @Column(name = "deleted")
    private boolean deleted = false;

    /**
     * Constructor.
     *
     * @param key
     *            of the type
     * @param name
     *            of the type
     * @param description
     *            of the type
     * @param maxAssignments
     *            assignments to a DS
     */
    public SoftwareModuleType(final String key, final String name, final String description, final int maxAssignments) {
        this(key, name, description, maxAssignments, null);
    }

    /**
     * Constructor.
     *
     * @param key
     *            of the type
     * @param name
     *            of the type
     * @param description
     *            of the type
     * @param maxAssignments
     *            assignments to a DS
     * @param colour
     *            of the type. It will be null by default
     */
    public SoftwareModuleType(final String key, final String name, final String description, final int maxAssignments,
            final String colour) {
        super();
        this.key = key;
        this.maxAssignments = maxAssignments;
        setDescription(description);
        setName(name);
        this.colour = colour;
    }

    /**
     * Default Constructor.
     */
    public SoftwareModuleType() {
        super();
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the max
     */
    public int getMaxAssignments() {
        return maxAssignments;
    }

    /**
     * @return the deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @param deleted
     *            the deleted to set
     */
    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    /**
     *
     * @return the software type color
     */
    public String getColour() {
        return colour;
    }

    /**
     *
     * @param colour
     *            the col
     */
    public void setColour(final String colour) {
        this.colour = colour;
    }

    @Override
    public String toString() {
        return "SoftwareModuleType [key=" + key + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

}
