/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Tag;

/**
 * Builder to create a new {@link Tag} entry. Defines all fields that can be set
 * at creation time. Other fields are set by the repository automatically, e.g.
 * {@link BaseEntity#getCreatedAt()}.
 */
public interface TagCreate<T extends Tag> extends RepositoryManagement.Builder<T> {

    /**
     * @param name for {@link Tag#getName()}
     * @return updated builder instance
     */
    TagCreate<T> name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description for {@link Tag#getDescription()}
     * @return updated builder instance
     */
    TagCreate<T> description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour for {@link Tag#getColour()}
     * @return updated builder instance
     */
    TagCreate<T> colour(@Size(max = Tag.COLOUR_MAX_SIZE) String colour);

    /**
     * @return peek on current state of {@link Tag} in the builder
     */
    T build();
}
