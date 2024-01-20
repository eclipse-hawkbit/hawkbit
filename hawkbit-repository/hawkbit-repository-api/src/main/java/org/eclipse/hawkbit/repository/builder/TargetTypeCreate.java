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

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.Type;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Collection;
import java.util.Collections;

/**
 * Builder to create a new {@link TargetType} entry. Defines all fields
 * that can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface TargetTypeCreate {

    /**
     * @param name
     *            for {@link TargetType#getName()}
     * @return updated builder instance
     */
    TargetTypeCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotEmpty String name);

    /**
     * @param description
     *            for {@link TargetType#getDescription()}
     * @return updated builder instance
     */
    TargetTypeCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param key
     *            for {@link TargetType#getName()}
     * @return updated builder instance
     */
    TargetTypeCreate key(@Size(min = 1, max = Type.KEY_MAX_SIZE) @NotEmpty String key);

    /**
     * @param colour
     *            for {@link TargetType#getColour()}
     * @return updated builder instance
     */
    TargetTypeCreate colour(@Size(max = Type.COLOUR_MAX_SIZE) String colour);

    /**
     * @param compatible
     *            for {@link TargetType#getCompatibleDistributionSetTypes()}
     * @return updated builder instance
     */
    TargetTypeCreate compatible(@NotEmpty Collection<Long> compatible);

    /**
     * @param compatible
     *            for {@link TargetType#getCompatibleDistributionSetTypes()}
     * @return updated builder instance
     */
    default TargetTypeCreate compatible(@NotNull final Long compatible) {
        return compatible(Collections.singletonList(compatible));
    }

    /**
     * @param compatible
     *            for {@link TargetType#getCompatibleDistributionSetTypes()}
     * @return updated builder instance
     */
    default TargetTypeCreate compatible(@NotNull final DistributionSetType compatible) {
        return compatible(compatible.getId());
    }

    /**
     * @return peek on current state of {@link TargetType} in the
     *         builder
     */
    TargetType build();
}
