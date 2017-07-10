/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Builder to create a new {@link DistributionSetType} entry. Defines all fields
 * that can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface DistributionSetTypeCreate {

    /**
     * @param key
     *            for {@link DistributionSetType#getKey()}
     * @return updated builder instance
     */
    DistributionSetTypeCreate key(@Size(min = 1, max = DistributionSetType.KEY_MAX_SIZE) @NotNull String key);

    /**
     * @param name
     *            for {@link DistributionSetType#getName()}
     * @return updated builder instance
     */
    DistributionSetTypeCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description
     *            for {@link DistributionSetType#getDescription()}
     * @return updated builder instance
     */
    DistributionSetTypeCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour
     *            for {@link DistributionSetType#getColour()}
     * @return updated builder instance
     */
    DistributionSetTypeCreate colour(@Size(max = DistributionSetType.COLOUR_MAX_SIZE) String colour);

    /**
     * @param mandatory
     *            for {@link DistributionSetType#getMandatoryModuleTypes()}
     * @return updated builder instance
     */
    DistributionSetTypeCreate mandatory(Collection<Long> mandatory);

    /**
     * @param mandatory
     *            for {@link DistributionSetType#getMandatoryModuleTypes()}
     * @return updated builder instance
     */
    default DistributionSetTypeCreate mandatory(final Long mandatory) {
        return mandatory(Arrays.asList(mandatory));
    }

    /**
     * @param mandatory
     *            for {@link DistributionSetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    default DistributionSetTypeCreate mandatory(final SoftwareModuleType mandatory) {
        return mandatory(Optional.ofNullable(mandatory).map(SoftwareModuleType::getId).orElse(null));
    }

    /**
     * @param optional
     *            for {@link DistributionSetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    DistributionSetTypeCreate optional(Collection<Long> optional);

    /**
     * @param optional
     *            for {@link DistributionSetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    default DistributionSetTypeCreate optional(final Long optional) {
        return optional(Arrays.asList(optional));
    }

    /**
     * @param optional
     *            for {@link DistributionSetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    default DistributionSetTypeCreate optional(final SoftwareModuleType optional) {
        return optional(Optional.ofNullable(optional).map(SoftwareModuleType::getId).orElse(null));
    }

    /**
     * @return peek on current state of {@link DistributionSetType} in the
     *         builder
     */
    DistributionSetType build();
}
