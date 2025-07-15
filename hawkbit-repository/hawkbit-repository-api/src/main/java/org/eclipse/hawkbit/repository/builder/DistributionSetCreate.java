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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Type;

/**
 * Builder to create a new {@link DistributionSet} entry. Defines all fields
 * that can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 */
public interface DistributionSetCreate<T extends DistributionSet> extends RepositoryManagement.Builder<T> {

    /**
     * @param name for {@link DistributionSet#getName()}
     * @return updated builder instance
     */
    DistributionSetCreate<T> name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param version for {@link DistributionSet#getVersion()}
     * @return updated builder instance
     */
    DistributionSetCreate<T> version(@Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE) @NotNull String version);

    /**
     * @param description for {@link DistributionSet#getDescription()}
     * @return updated builder instance
     */
    DistributionSetCreate<T> description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param typeKey for {@link DistributionSet#getType()}
     * @return updated builder instance
     */
    DistributionSetCreate<T> type(@Size(min = 1, max = Type.KEY_MAX_SIZE) @NotNull String typeKey);

    /**
     * @param type for {@link DistributionSet#getType()}
     * @return updated builder instance
     */
    default DistributionSetCreate<T> type(@NotNull final DistributionSetType type) {
        return type(type.getKey());
    }

    /**
     * @param modules for {@link DistributionSet#getModules()}
     * @return updated builder instance
     */
    DistributionSetCreate<T> modules(Collection<Long> modules);

    /**
     * @param requiredMigrationStep for {@link DistributionSet#isRequiredMigrationStep()}
     * @return updated builder instance
     */
    DistributionSetCreate<T> requiredMigrationStep(Boolean requiredMigrationStep);

    /**
     * @return peek on current state of {@link DistributionSet} in the builder
     */
    T build();
}