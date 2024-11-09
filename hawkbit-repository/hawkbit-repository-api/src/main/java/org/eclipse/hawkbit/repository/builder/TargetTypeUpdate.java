/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.Type;

/**
 * Builder to update an existing {@link TargetType} entry. Defines all
 * fields that can be updated.
 */
public interface TargetTypeUpdate {

    /**
     * @param description for {@link TargetType#getDescription()}
     * @return updated builder instance
     */
    TargetTypeUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour for {@link TargetType#getColour()}
     * @return updated builder instance
     */
    TargetTypeUpdate colour(@Size(max = Type.COLOUR_MAX_SIZE) String colour);

    /**
     * @param name Name
     * @return updated builder instance
     */
    TargetTypeUpdate name(@Size(max = TargetType.NAME_MAX_SIZE) String name);
}
