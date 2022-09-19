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
public interface TargetType extends NamedEntity {
    /**
     * Maximum length of color in Management UI.
     */
    int COLOUR_MAX_SIZE = 16;

    /**
     * @return immutable set of optional {@link DistributionSetType}s
     */
    Set<DistributionSetType> getCompatibleDistributionSetTypes();

    /**
     * @return immutable set of optional {@link Target}s
     */
    Set<Target> getTargets();

    /**
     * Checks if the given {@link DistributionSetType} is in
     * {@link #getCompatibleDistributionSetTypes()}.
     *
     * @param distributionSetType
     *            search for
     * @return <code>true</code> if found
     */
    default boolean containsCompatibleDistributionSetType(final DistributionSetType distributionSetType) {
        return containsCompatibleDistributionSetType(distributionSetType.getId());
    }

    /**
     * Checks if the given {@link DistributionSetType} is in
     * {@link #getCompatibleDistributionSetTypes()}.
     *
     * @param distributionSetTypeId
     *            search by {@link DistributionSetType#getId()}
     * @return <code>true</code> if found
     */
    default boolean containsCompatibleDistributionSetType(final Long distributionSetTypeId) {
        return getCompatibleDistributionSetTypes().stream().anyMatch(element -> element.getId().equals(distributionSetTypeId));
    }

    /**
     * Unassigns a {@link DistributionSetType} from {@link TargetType}
     * 
     * @param dsTypeId
     *            that will be removed from {@link TargetType}
     * @return the resulting target type
     */
    TargetType removeDistributionSetType(final Long dsTypeId);

    /**
     * @return get color code to be used in management UI views.
     */
    String getColour();
}
