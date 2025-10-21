/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Set;

/**
 * A {@link DistributionSetType} is an abstract definition for {@link DistributionSet} that defines what {@link SoftwareModule}s can be
 * added (optional) to {@link DistributionSet} of that type or have to added (mandatory) in order to be considered complete. Only complete DS
 * can be assigned to a {@link Target}.
 */
public interface DistributionSetType extends Type {

    /**
     * @return immutable set of {@link SoftwareModuleType}s that need to be in a {@link DistributionSet} of this type to be
     *         {@link DistributionSet#isComplete()}.
     */
    Set<SoftwareModuleType> getMandatoryModuleTypes();

    /**
     * @return immutable set of optional {@link SoftwareModuleType}s that can be in a {@link DistributionSet} of this type.
     */
    Set<SoftwareModuleType> getOptionalModuleTypes();

    /**
     * Checks if the given {@link SoftwareModuleType} is in this {@link DistributionSetType}.
     *
     * @param softwareModuleType search for
     * @return <code>true</code> if found
     */
    default boolean containsModuleType(final SoftwareModuleType softwareModuleType) {
        return containsMandatoryModuleType(softwareModuleType) || containsOptionalModuleType(softwareModuleType);
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in {@link #getMandatoryModuleTypes()}.
     *
     * @param softwareModuleType search for
     * @return <code>true</code> if found
     */
    default boolean containsMandatoryModuleType(final SoftwareModuleType softwareModuleType) {
        return getMandatoryModuleTypes().stream().anyMatch(element -> element.getId().equals(softwareModuleType.getId()));
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in {@link #getOptionalModuleTypes()}.
     *
     * @param softwareModuleType search for
     * @return <code>true</code> if found
     */
    default boolean containsOptionalModuleType(final SoftwareModuleType softwareModuleType) {
        return getOptionalModuleTypes().stream().anyMatch(element -> element.getId().equals(softwareModuleType.getId()));
    }
}