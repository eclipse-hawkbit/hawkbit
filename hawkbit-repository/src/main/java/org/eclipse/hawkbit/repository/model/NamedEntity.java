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
import javax.persistence.MappedSuperclass;

/**
 * {@link TenantAwareBaseEntity} extension for all entities that are named in
 * addition to their technical ID.
 */
@MappedSuperclass
public abstract class NamedEntity extends TenantAwareBaseEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", nullable = true, length = 512)
    private String description;

    /**
     * Default constructor.
     */
    public NamedEntity() {
        super();
    }

    /**
     * Parameterized constructor.
     * 
     * @param name
     *            of the {@link NamedEntity}
     * @param description
     *            of the {@link NamedEntity}
     */
    public NamedEntity(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NamedEntity other = (NamedEntity) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
