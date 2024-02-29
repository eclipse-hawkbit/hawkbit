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

import lombok.Data;

/**
 * object that holds information about the count of affected rollouts,
 * auto-assignments and actions, when a list of distribution sets gets
 * invalidated
 */
@Data
public class DistributionSetInvalidationCount {

    private final long rolloutsCount;
    private final long autoAssignmentCount;
    private final long actionCount;

    public DistributionSetInvalidationCount(final long rolloutsCount, final long autoAssignmentCount,
            final long actionCount) {
        this.rolloutsCount = rolloutsCount;
        this.autoAssignmentCount = autoAssignmentCount;
        this.actionCount = actionCount;
    }
}