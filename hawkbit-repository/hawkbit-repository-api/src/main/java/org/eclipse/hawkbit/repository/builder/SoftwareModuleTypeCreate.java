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
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Type;

/**
 * Builder to create a new {@link SoftwareModuleType} entry. Defines all fields that can be set at creation time. Other fields are set
 * by the repository automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 */
public interface SoftwareModuleTypeCreate<T extends SoftwareModuleType> extends RepositoryManagement.Builder<T> {

    /**
     * @param key for {@link SoftwareModuleType#getKey()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate<T> key(@Size(min = 1, max = Type.KEY_MAX_SIZE) @NotNull String key);

    /**
     * @param name for {@link SoftwareModuleType#getName()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate<T> name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description for {@link SoftwareModuleType#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate<T> description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour for {@link SoftwareModuleType#getColour()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate<T> colour(@Size(max = Type.COLOUR_MAX_SIZE) String colour);

    /**
     * @param maxAssignments for {@link SoftwareModuleType#getMaxAssignments()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate<T> maxAssignments(int maxAssignments);

    /**
     * @return peek on current state of {@link SoftwareModuleType} in the
     *         builder
     */
    T build();
}