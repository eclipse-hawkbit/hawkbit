/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Set;

/**
 * A {@link TargetType} is an abstract definition for
 * {@link Target}
 *
 */
public interface TargetType extends Type{
    /**
     * @return immutable set of {@link DistributionSetType}s
     */
    Set<DistributionSetType> getMandatoryModuleTypes();

    /**
     * @return immutable set of optional {@link DistributionSetType}s
     */
    Set<DistributionSetType> getOptionalModuleTypes();

    /**
     * Checks if the given {@link DistributionSetType} is in this
     * {@link DistributionSetType}.
     *
     * @param distributionSetType
     *            search for
     * @return <code>true</code> if found
     */
    default boolean containsModuleType(final DistributionSetType distributionSetType) {
        return containsMandatoryModuleType(distributionSetType) || containsOptionalModuleType(distributionSetType);
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType}.
     *
     * @param distributionSetTypeId
     *            search for by {@link DistributionSetType#getId()}
     * @return <code>true</code> if found
     */
    default boolean containsModuleType(final Long distributionSetTypeId) {
        return containsMandatoryModuleType(distributionSetTypeId) || containsOptionalModuleType(distributionSetTypeId);
    }

    /**
     * Checks if the given {@link DistributionSetType} is in
     * {@link #getMandatoryModuleTypes()}.
     *
     * @param distributionSetType
     *            search for
     * @return <code>true</code> if found
     */
    default boolean containsMandatoryModuleType(final DistributionSetType distributionSetType) {
        return containsMandatoryModuleType(distributionSetType.getId());
    }

    /**
     * Checks if the given {@link DistributionSetType} is in
     * {@link #getMandatoryModuleTypes()}.
     *
     * @param distributionSetTypeId
     *            search for by {@link DistributionSetType#getId()}
     * @return <code>true</code> if found
     */
    default boolean containsMandatoryModuleType(final Long distributionSetTypeId) {
        return getMandatoryModuleTypes().stream().anyMatch(element -> element.getId().equals(distributionSetTypeId));
    }

    /**
     * Checks if the given {@link DistributionSetType} is in
     * {@link #getOptionalModuleTypes()}.
     *
     * @param distributionSetType
     *            search for
     * @return <code>true</code> if found
     */
    default boolean containsOptionalModuleType(final DistributionSetType distributionSetType) {
        return containsOptionalModuleType(distributionSetType.getId());
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in
     * {@link #getOptionalModuleTypes()}.
     *
     * @param distributionSetTypeId
     *            search by {@link DistributionSetType#getId()}
     * @return <code>true</code> if found
     */
    default boolean containsOptionalModuleType(final Long distributionSetTypeId) {
        return getOptionalModuleTypes().stream().anyMatch(element -> element.getId().equals(distributionSetTypeId));
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
     * @param target
     *            to check for completeness
     * @return <code>true</code> if the all mandatory target types are
     *         in the system.
     */
    boolean checkComplete(Target target);
}
