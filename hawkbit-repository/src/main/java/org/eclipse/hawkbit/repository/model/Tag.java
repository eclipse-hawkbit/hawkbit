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

import org.springframework.hateoas.Identifiable;

/**
 * A Tag can be used as describing and organizational meta information for any
 * kind of entity.
 *
 */
@MappedSuperclass
public abstract class Tag extends NamedEntity implements Identifiable<Long> {
    private static final long serialVersionUID = 1L;

    @Column(name = "colour", nullable = true, length = 16)
    private String colour;

    protected Tag() {
        super();
    }

    /**
     * Public constructor.
     *
     * @param name
     *            of the {@link Tag}
     * @param description
     *            of the {@link Tag}
     * @param colour
     *            of tag in UI
     */
    public Tag(final String name, final String description, final String colour) {
        super(name, description);
        this.colour = colour;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(final String colour) {
        this.colour = colour;
    }

    @Override
    public String toString() {
        return "Tag [getOptLockRevision()=" + getOptLockRevision() + ", getId()=" + getId() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Tag)) {
            return false;
        }

        return true;
    }
}
