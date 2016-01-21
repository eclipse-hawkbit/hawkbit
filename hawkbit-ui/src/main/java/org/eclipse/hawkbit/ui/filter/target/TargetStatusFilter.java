/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.filter.target;

import java.util.List;

import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.filter.FilterExpression;

/**
 *
 *
 *
 */
public class TargetStatusFilter implements FilterExpression {

    private final List<TargetUpdateStatus> targetUpdateStatus;

    /**
     * @param target
     *            the target to check the update status against
     * @param updateStatus
     *            the target update status to check against the given target
     */
    public TargetStatusFilter(final List<TargetUpdateStatus> updateStatus) {
        this.targetUpdateStatus = updateStatus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.filter.FilterExpression#evaluate()
     */
    @Override
    public boolean doFilter() {
        if (targetUpdateStatus.isEmpty()) {
            return false;
        }
        return true;
    }
}
