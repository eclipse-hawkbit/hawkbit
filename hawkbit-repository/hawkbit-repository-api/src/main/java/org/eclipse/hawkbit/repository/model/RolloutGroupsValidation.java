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

import java.util.List;

/**
 * Represents information to validate the correct distribution of targets into rollout groups.
 *
 * @param totalTargets The total amount of targets in a {@link Rollout}
 * @param targetsPerGroup A list containing the count of targets for each {@link RolloutGroup}
 */
public record RolloutGroupsValidation(long totalTargets, List<Long> targetsPerGroup) {

    /**
     * @return the count of targets that are in groups
     */
    public long getTargetsInGroups() {
        return targetsPerGroup.stream().mapToLong(Long::longValue).sum();
    }

    /**
     * @return whether the groups contain all targets
     */
    public boolean isValid() {
        return totalTargets == getTargetsInGroups();
    }
}