/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;

/**
 * Builder to update an existing {@link DistributionSet} entry. Defines all
 * fields that can be updated.
 *
 */
public interface DistributionSetUpdate {
    /**
     * @param name
     *            for {@link DistributionSet#getName()}
     * @return updated builder instance
     */
    DistributionSetUpdate name(String name);

    /**
     * @param version
     *            for {@link DistributionSet#getVersion()}
     * @return updated builder instance
     */
    DistributionSetUpdate version(String version);

    /**
     * @param description
     *            for {@link DistributionSet#getDescription()}
     * @return updated builder instance
     */
    DistributionSetUpdate description(String description);

    /**
     * @param typeKey
     *            for {@link DistributionSet#getType()}
     * @return updated builder instance
     */
    DistributionSetUpdate type(String typeKey);

    /**
     * @param type
     *            for {@link DistributionSet#getType()}
     * @return updated builder instance
     */
    default DistributionSetUpdate type(final DistributionSetType type) {
        return type(Optional.ofNullable(type).map(DistributionSetType::getKey).orElse(null));
    }

    /**
     * @param requiredMigrationStep
     *            for {@link DistributionSet#isRequiredMigrationStep()}
     * @return updated builder instance
     */
    DistributionSetUpdate requiredMigrationStep(Boolean requiredMigrationStep);
}
