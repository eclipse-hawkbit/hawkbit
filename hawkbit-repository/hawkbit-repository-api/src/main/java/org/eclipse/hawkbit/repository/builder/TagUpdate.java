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

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Tag;

/**
 * Builder to update an existing {@link Tag} entry. Defines all fields that can
 * be updated.
 *
 */
public interface TagUpdate {
    /**
     * @param name
     *            for {@link Tag#getName()}
     * @return updated builder instance
     */
    TagUpdate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description
     *            for {@link Tag#getDescription()}
     * @return updated builder instance
     */
    TagUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour
     *            for {@link Tag#getColour()}
     * @return updated builder instance
     */
    TagUpdate colour(@Size(max = Tag.COLOUR_MAX_SIZE) String colour);

}
