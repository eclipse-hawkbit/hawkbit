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

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;

/**
 * Extension for {@link NamedEntity} that are versioned.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Default constructor needed for JPA entities
@Setter
@Getter
@ToString(callSuper = true)
@MappedSuperclass
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public abstract class AbstractJpaNamedVersionedEntity extends AbstractJpaNamedEntity implements NamedVersionedEntity {

    @Column(name = "version", nullable = false, length = NamedVersionedEntity.VERSION_MAX_SIZE)
    @Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE)
    @NotNull
    private String version;

    AbstractJpaNamedVersionedEntity(final String name, final String version, final String description) {
        super(name, description);
        this.version = version;
    }
}