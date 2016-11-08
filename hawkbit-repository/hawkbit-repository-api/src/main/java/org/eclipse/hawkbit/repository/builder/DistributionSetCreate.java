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

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.hibernate.validator.constraints.NotEmpty;

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
    DistributionSetCreate name(@NotEmpty String name);

    /**
     * @param version
     *            for {@link DistributionSet#getVersion()}
     * @return updated builder instance
     */
    DistributionSetCreate version(@NotEmpty String version);

    /**
     * @param description
     *            for {@link DistributionSet#getDescription()}
     * @return updated builder instance
     */
    DistributionSetCreate description(String description);

    /**
     * @param typeKey
     *            for {@link DistributionSet#getType()}
     * @return updated builder instance
     */
    DistributionSetCreate type(@NotEmpty String typeKey);

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
