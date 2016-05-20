/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;

/**
 * Extension for {@link NamedEntity} that are versioned.
 *
 */
@MappedSuperclass
public abstract class JpaNamedVersionedEntity extends JpaNamedEntity implements NamedVersionedEntity {
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
    public JpaNamedVersionedEntity(final String name, final String version, final String description) {
        super(name, description);
        this.version = version;
    }

    JpaNamedVersionedEntity() {
        super();
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(final String version) {
        this.version = version;
    }
}
