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
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * {@link TenantAwareBaseEntity} extension for all entities that are named in
 * addition to their technical ID.
 */
@MappedSuperclass
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public abstract class AbstractJpaNamedEntity extends AbstractJpaTenantAwareBaseEntity implements NamedEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, length = NamedEntity.NAME_MAX_SIZE)
    @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
    @NotNull
    private String name;

    @Column(name = "description", nullable = true, length = NamedEntity.DESCRIPTION_MAX_SIZE)
    @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
    private String description;

    /**
     * Default constructor.
     */
    public AbstractJpaNamedEntity() {
        // Default constructor needed for JPA entities
    }

    /**
     * Parameterized constructor.
     *
     * @param name
     *            of the {@link NamedEntity}
     * @param description
     *            of the {@link NamedEntity}
     */
    AbstractJpaNamedEntity(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
