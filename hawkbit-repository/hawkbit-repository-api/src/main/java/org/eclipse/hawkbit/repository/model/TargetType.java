/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
 * A {@link TargetType} is an abstract definition for {@link Target}
 */
public interface TargetType extends Type {

    /**
     * Target type doesn't support soft-delete so all target type instances are not deleted.
     *
     * @return <code>false</code>
     */
    @Override
    default boolean isDeleted() {
        return false;
    }

    /**
     * Gets compatible distribution set types
     *
     * @return immutable set of optional {@link DistributionSetType}s
     */
    Set<DistributionSetType> getDistributionSetTypes();

    /**
     * Unassigns a {@link DistributionSetType} from {@link TargetType}
     *
     * @param dsTypeId that will be removed from {@link TargetType}
     */
    void removeDistributionSetType(final Long dsTypeId);
}