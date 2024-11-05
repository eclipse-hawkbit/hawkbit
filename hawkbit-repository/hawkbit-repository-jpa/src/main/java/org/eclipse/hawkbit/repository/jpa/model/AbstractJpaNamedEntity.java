/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, length = NAME_MAX_SIZE)
    @Size(min = 1, max = NAME_MAX_SIZE)
    @NotNull
    private String name;

    @Column(name = "description", length = DESCRIPTION_MAX_SIZE)
    @Size(max = DESCRIPTION_MAX_SIZE)
    private String description;

    /**
     * Default constructor.
     */
    protected AbstractJpaNamedEntity() {
        // Default constructor needed for JPA entities
    }

    /**
     * Parameterized constructor.
     *
     * @param name of the {@link NamedEntity}
     * @param description of the {@link NamedEntity}
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

    public void setName(final String name) {
        this.name = name;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
