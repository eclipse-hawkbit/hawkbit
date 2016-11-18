/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.hibernate.validator.constraints.NotEmpty;

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
    SoftwareModuleTypeCreate key(@NotEmpty String key);

    /**
     * @param name
     *            for {@link SoftwareModuleType#getName()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate name(@NotEmpty String name);

    /**
     * @param description
     *            for {@link SoftwareModuleType#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate description(String description);

    /**
     * @param colour
     *            for {@link SoftwareModuleType#getColour()}
     * @return updated builder instance
     */
    SoftwareModuleTypeCreate colour(String colour);

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
