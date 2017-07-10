/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Builder to create a new {@link SoftwareModuleType} entry. Defines all fields
 * that can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface SoftwareModuleTypeCreate {
    /**
     * @param key
     *            for {@link SoftwareModuleType#getKey()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate key(@Size(min = 1, max = SoftwareModuleType.KEY_MAX_SIZE) @NotNull String key);

    /**
     * @param name
     *            for {@link SoftwareModuleType#getName()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description
     *            for {@link SoftwareModuleType#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour
     *            for {@link SoftwareModuleType#getColour()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate colour(@Size(max = SoftwareModuleType.COLOUR_MAX_SIZE) String colour);

    /**
     * @param maxAssignments
     *            for {@link SoftwareModuleType#getMaxAssignments()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate maxAssignments(int maxAssignments);

    /**
     * @return peek on current state of {@link SoftwareModuleType} in the
     *         builder
     */
    SoftwareModuleType build();
}
