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

import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * Represents information to validate the correct distribution of targets to
 * rollout groups.
 */
@Data
public class RolloutGroupsValidation {

    /**
     * The total amount of targets in a {@link Rollout}
     */
    private final long totalTargets;

    /**
     * A list containing the count of targets for each {@link RolloutGroup}
     */
    private final List<Long> targetsPerGroup;

    /**
     * Instantiates a new validation result
     *
     * @param totalTargets The total amount of targets in a {@link Rollout}
     * @param targetsPerGroup A list containing the count of targets for each
     *         {@link RolloutGroup}
     */
    public RolloutGroupsValidation(final long totalTargets, @NotNull final List<Long> targetsPerGroup) {
        this.totalTargets = totalTargets;
        this.targetsPerGroup = targetsPerGroup;
    }

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