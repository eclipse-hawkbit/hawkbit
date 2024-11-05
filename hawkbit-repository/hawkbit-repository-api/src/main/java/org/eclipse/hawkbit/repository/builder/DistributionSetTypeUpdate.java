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

import java.util.Collection;

import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;

/**
 * Builder to update an existing {@link DistributionSetType} entry. Defines all
 * fields that can be updated.
 */
public interface DistributionSetTypeUpdate {

    /**
     * @param description for {@link DistributionSetType#getDescription()}
     * @return updated builder instance
     */
    DistributionSetTypeUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour for {@link DistributionSetType#getColour()}
     * @return updated builder instance
     */
    DistributionSetTypeUpdate colour(@Size(max = DistributionSetType.COLOUR_MAX_SIZE) String colour);

    /**
     * @param mandatory for {@link DistributionSetType#getMandatoryModuleTypes()}
     * @return updated builder instance
     */
    DistributionSetTypeUpdate mandatory(Collection<Long> mandatory);

    /**
     * @param optional for {@link DistributionSetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    DistributionSetTypeUpdate optional(Collection<Long> optional);
}
