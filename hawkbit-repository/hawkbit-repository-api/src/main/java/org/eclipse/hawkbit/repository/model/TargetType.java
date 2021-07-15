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
     * @return immutable set of optional {@link DistributionSetType}s
     */
    Set<DistributionSetType> getOptionalSetTypes();


    /**
     * Checks if the given {@link DistributionSetType} is in
     * {@link #getOptionalSetTypes()}.
     *
     * @param distributionSetType
     *            search for
     * @return <code>true</code> if found
     */
    default boolean containsOptionalSetType(final DistributionSetType distributionSetType) {
        return containsOptionalSetType(distributionSetType.getId());
    }

    /**
     * Checks if the given {@link DistributionSetType} is in
     * {@link #getOptionalSetTypes()}.
     *
     * @param distributionSetTypeId
     *            search by {@link DistributionSetType#getId()}
     * @return <code>true</code> if found
     */
    default boolean containsOptionalSetType(final Long distributionSetTypeId) {
        return getOptionalSetTypes().stream().anyMatch(element -> element.getId().equals(distributionSetTypeId));
    }

    /**
     * Compares the modules of this {@link DistributionSetType} and the given
     * one.
     *
     * @param dsType
     *            to compare with
     * @return <code>true</code> if the lists are identical.
     */
    boolean areSetEntriesIdentical(DistributionSetType dsType);

    /**
     * @param target
     *            to check for completeness
     * @return <code>true</code> if the all mandatory target types are
     *         in the system.
     */
    boolean checkComplete(Target target);
}
