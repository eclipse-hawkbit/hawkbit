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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;

/**
 * Extension for {@link NamedEntity} that are versioned.
 *
 */
@MappedSuperclass
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public abstract class AbstractJpaNamedVersionedEntity extends AbstractJpaNamedEntity implements NamedVersionedEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "version", nullable = false, length = NamedVersionedEntity.VERSION_MAX_SIZE)
    @Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE)
    @NotNull
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
    AbstractJpaNamedVersionedEntity(final String name, final String version, final String description) {
        super(name, description);
        this.version = version;
    }

    AbstractJpaNamedVersionedEntity() {
        // Default constructor needed for JPA entities
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }
}
