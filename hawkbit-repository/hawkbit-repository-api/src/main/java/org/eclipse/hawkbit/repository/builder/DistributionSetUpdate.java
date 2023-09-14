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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;

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
    DistributionSetUpdate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param version
     *            for {@link DistributionSet#getVersion()}
     * @return updated builder instance
     */
    DistributionSetUpdate version(@Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE) @NotNull String version);

    /**
     * @param description
     *            for {@link DistributionSet#getDescription()}
     * @return updated builder instance
     */
    DistributionSetUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param requiredMigrationStep
     *            for {@link DistributionSet#isRequiredMigrationStep()}
     * @return updated builder instance
     */
    DistributionSetUpdate requiredMigrationStep(Boolean requiredMigrationStep);
}
