/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Collection;
import java.util.Optional;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;

/**
 * Builder to create a new {@link DistributionSet} entry. Defines all fields
 * that can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface DistributionSetCreate {

    /**
     * @param name
     *            for {@link DistributionSet#getName()}
     * @return updated builder instance
     */
    DistributionSetCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param version
     *            for {@link DistributionSet#getVersion()}
     * @return updated builder instance
     */
    DistributionSetCreate version(@Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE) @NotNull String version);

    /**
     * @param description
     *            for {@link DistributionSet#getDescription()}
     * @return updated builder instance
     */
    DistributionSetCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param typeKey
     *            for {@link DistributionSet#getType()}
     * @return updated builder instance
     */
    DistributionSetCreate type(@Size(min = 1, max = DistributionSetType.KEY_MAX_SIZE) @NotNull String typeKey);

    /**
     * @param type
     *            for {@link DistributionSet#getType()}
     * @return updated builder instance
     */
    default DistributionSetCreate type(final DistributionSetType type) {
        return type(Optional.ofNullable(type).map(DistributionSetType::getKey).orElse(null));
    }

    /**
     * @param modules
     *            for {@link DistributionSet#getModules()}
     * @return updated builder instance
     */
    DistributionSetCreate modules(Collection<Long> modules);

    /**
     * @param requiredMigrationStep
     *            for {@link DistributionSet#isRequiredMigrationStep()}
     * @return updated builder instance
     */
    DistributionSetCreate requiredMigrationStep(Boolean requiredMigrationStep);

    /**
     * @return peek on current state of {@link DistributionSet} in the builder
     */
    DistributionSet build();
}
