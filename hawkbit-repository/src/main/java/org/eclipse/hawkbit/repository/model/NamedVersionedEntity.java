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
 * Extension for {@link NamedEntity} that are versioned.
 *
 */
@MappedSuperclass
public abstract class NamedVersionedEntity extends NamedEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "version", nullable = false, length = 64)
    private String version;

    /**
     * parameterized constructor.
     *
     * @param name
     *            of the entity
     * @param version
     *            of the entity
     * @param description
     */
    public NamedVersionedEntity(final String name, final String version, final String description) {
        super(name, description);
        this.version = version;
    }

    NamedVersionedEntity() {
        super();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }
}
