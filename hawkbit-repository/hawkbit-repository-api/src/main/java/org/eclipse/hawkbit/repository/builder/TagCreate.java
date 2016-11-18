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
import org.eclipse.hawkbit.repository.model.Tag;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Builder to create a new {@link Tag} entry. Defines all fields that can be set
 * at creation time. Other fields are set by the repository automatically, e.g.
 * {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface TagCreate {
    /**
     * @param name
     *            for {@link Tag#getName()}
     * @return updated builder instance
     */
    TagCreate name(@NotEmpty String name);

    /**
     * @param description
     *            for {@link Tag#getDescription()}
     * @return updated builder instance
     */
    TagCreate description(String description);

    /**
     * @param colour
     *            for {@link Tag#getColour()}
     * @return updated builder instance
     */
    TagCreate colour(String colour);

    /**
     * @return peek on current state of {@link Tag} in the builder
     */
    Tag build();
}
