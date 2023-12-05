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

import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.Type;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serial;

/**
 * {@link TenantAwareBaseEntity} extension for all entities that are named in
 * addition to their technical ID.
 */
@MappedSuperclass
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public abstract class AbstractJpaTypeEntity extends AbstractJpaNamedEntity implements Type {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "type_key", nullable = false, updatable = false, length = KEY_MAX_SIZE)
    @Size(min = 1, max = KEY_MAX_SIZE)
    @NotNull
    private String key;

    @Column(name = "colour", length = COLOUR_MAX_SIZE)
    @Size(max = COLOUR_MAX_SIZE)
    private String colour;

    /**
     * Default constructor.
     */
    protected AbstractJpaTypeEntity() {
        // Default constructor needed for JPA entities
    }

    /**
     * Parameterized constructor.
     *
     * @param key
     *            of the {@link Type}
     * @param colour
     *            of the {@link Type}
     */
    AbstractJpaTypeEntity(final String name, final String description, final String key, final String colour) {
        super(name, description);
        this.key = key;
        this.colour = colour;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getColour() {
        return colour;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public void setColour(final String colour) {
        this.colour = colour;
    }
}
