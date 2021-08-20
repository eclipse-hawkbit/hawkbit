/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;

import javax.validation.constraints.Size;

/**
 * Builder to update an existing {@link TargetType} entry. Defines all
 * fields that can be updated.
 *
 */
public interface TargetTypeUpdate {
    /**
     * @param description
     *            for {@link TargetType#getDescription()}
     * @return updated builder instance
     */
    TargetTypeUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour
     *            for {@link TargetType#getColour()}
     * @return updated builder instance
     */
    TargetTypeUpdate colour(@Size(max = TargetType.COLOUR_MAX_SIZE) String colour);

    /**
     * @param name
     *            Name
     * @return updated builder instance
     */
    TargetTypeUpdate name(@Size(max = TargetType.NAME_MAX_SIZE) String name);
}
