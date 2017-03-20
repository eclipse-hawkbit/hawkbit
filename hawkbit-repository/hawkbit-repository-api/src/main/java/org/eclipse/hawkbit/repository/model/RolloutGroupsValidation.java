/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Represents information to validate the correct distribution of targets to
 * rollout groups.
 */
public class RolloutGroupsValidation {

    /**
     * The total amount of targets in a {@link Rollout}
     */
    private long totalTargets;

    /**
     * A list containing the count of targets for each {@link RolloutGroup}
     */
    private List<Long> targetsPerGroup;

    /**
     * Instantiates a new validation result
     * 
     * @param totalTargets
     *            The total amount of targets in a {@link Rollout}
     * @param targetsPerGroup
     *            A list containing the count of targets for each
     *            {@link RolloutGroup}
     */
    public RolloutGroupsValidation(final long totalTargets, @NotNull final List<Long> targetsPerGroup) {
        this.totalTargets = totalTargets;
        this.targetsPerGroup = targetsPerGroup;
    }

    public long getTotalTargets() {
        return totalTargets;
    }

    public List<Long> getTargetsPerGroup() {
        return targetsPerGroup;
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
