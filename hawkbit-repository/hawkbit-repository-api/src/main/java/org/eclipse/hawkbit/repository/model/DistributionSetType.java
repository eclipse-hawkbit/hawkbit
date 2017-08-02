/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Set;

/**
 * A {@link DistributionSetType} is an abstract definition for
 * {@link DistributionSet} that defines what {@link SoftwareModule}s can be
 * added (optional) to {@link DistributionSet} of that type or have to added
 * (mandatory) in order to be considered complete. Only complete DS can be
 * assigned to a {@link Target}.
 *
 */
public interface DistributionSetType extends NamedEntity {
    /**
     * Maximum length of key.
     */
    int KEY_MAX_SIZE = 64;

    /**
     * Maximum length of colour in Management UI.
     */
    int COLOUR_MAX_SIZE = 16;

    /**
     * @return <code>true</code> if the type is deleted and only kept for
     *         history purposes.
     */
    boolean isDeleted();

    /**
     * @return immutable set of {@link SoftwareModuleType}s that need to be in a
     *         {@link DistributionSet} of this type to be
     *         {@link DistributionSet#isComplete()}.
     */
    Set<SoftwareModuleType> getMandatoryModuleTypes();

    /**
     * @return immutable set of optional {@link SoftwareModuleType}s that can be
     *         in a {@link DistributionSet} of this type.
     */
    Set<SoftwareModuleType> getOptionalModuleTypes();

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    default boolean containsModuleType(final SoftwareModuleType softwareModuleType) {
        return containsMandatoryModuleType(softwareModuleType) || containsOptionalModuleType(softwareModuleType);
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in
     * {@link #getMandatoryModuleTypes()}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    default boolean containsMandatoryModuleType(final SoftwareModuleType softwareModuleType) {
        return containsMandatoryModuleType(softwareModuleType.getId());
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in
     * {@link #getMandatoryModuleTypes()}.
     *
     * @param softwareModuleTypeId
     *            search for by {@link SoftwareModuleType#getId()}
     * @return <code>true</code> if found
     */
    default boolean containsMandatoryModuleType(final Long softwareModuleTypeId) {
        return getMandatoryModuleTypes().stream().anyMatch(element -> element.getId().equals(softwareModuleTypeId));
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in
     * {@link #getOptionalModuleTypes()}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    default boolean containsOptionalModuleType(final SoftwareModuleType softwareModuleType) {
        return containsOptionalModuleType(softwareModuleType.getId());
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in
     * {@link #getOptionalModuleTypes()}.
     *
     * @param softwareModuleTypeId
     *            search by {@link SoftwareModuleType#getId()}
     * @return <code>true</code> if found
     */
    default boolean containsOptionalModuleType(final Long softwareModuleTypeId) {
        return getOptionalModuleTypes().stream().anyMatch(element -> element.getId().equals(softwareModuleTypeId));
    }

    /**
     * Compares the modules of this {@link DistributionSetType} and the given
     * one.
     *
     * @param dsType
     *            to compare with
     * @return <code>true</code> if the lists are identical.
     */
    boolean areModuleEntriesIdentical(DistributionSetType dsType);

    /**
     * @return business key of this {@link DistributionSetType}.
     */
    String getKey();

    /**
     * @param distributionSet
     *            to check for completeness
     * @return <code>true</code> if the all mandatory software module types are
     *         in the system.
     */
    boolean checkComplete(DistributionSet distributionSet);

    /**
     * @return get color code to by used in management UI views.
     */
    String getColour();

}
