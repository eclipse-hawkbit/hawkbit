/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * object that holds information about the count of affected rollouts,
 * auto-assignments and actions, when a list of distribution sets gets
 * invalidated
 */
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

    public long getRolloutsCount() {
        return rolloutsCount;
    }

    public long getAutoAssignmentCount() {
        return autoAssignmentCount;
    }

    public long getActionCount() {
        return actionCount;
    }

}
