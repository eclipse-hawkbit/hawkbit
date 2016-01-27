/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * States with the target count of a rollout or rolloutgroup.
 *
 */
public class TotalTargetCountStatus {

    /**
     * Status of the total target counts.
     */
    public enum Status {
        READY, RUNNING, ERROR, FINISHED, CANCELLED, NOTSTARTED
    }

    private final Map<Status, Long> statusTotalCountMap = new HashMap<Status, Long>();

    /**
     * 
     * @param targetCountActionStatus
     * @param totalTargets
     */
    public TotalTargetCountStatus(final List<TotalTargetCountActionStatus> targetCountActionStatus,
            final Long totalTargets) {
        if (!mapActionStatusToTotalTargetCountStatus(targetCountActionStatus)) {
            statusTotalCountMap.put(TotalTargetCountStatus.Status.NOTSTARTED, totalTargets);
        }
    }

    /**
     * @return the statusTotalCountMap
     */
    public Map<Status, Long> getStatusTotalCountMap() {
        return statusTotalCountMap;
    }

    /**
     * 
     * @param rolloutStatus
     * @return
     */
    public Long getTotalCountByStatus(final Status rolloutStatus) {
        final Long count = statusTotalCountMap.get(rolloutStatus);
        return count == null ? Long.valueOf(0) : count;
    }

    /**
     * Populate all target status to a the given map
     * 
     * @param statusTotalCountMap
     *            the map
     * @param rolloutStatusCountItems
     *            all target statut with total count
     * @return <true> some state is populated <false> nothing is happend
     */
    private boolean mapActionStatusToTotalTargetCountStatus(
            final List<TotalTargetCountActionStatus> targetCountActionStatus) {
        if (targetCountActionStatus == null) {
            return false;
        }
        Long cancelledItemCount = 0L;
        Long runningItemsCount = 0L;

        for (final TotalTargetCountActionStatus item : targetCountActionStatus) {
            switch (item.getStatus()) {
            case SCHEDULED:
                statusTotalCountMap.put(Status.READY, item.getCount());
                break;
            case ERROR:
                statusTotalCountMap.put(Status.ERROR, item.getCount());
                break;
            case FINISHED:
                statusTotalCountMap.put(Status.FINISHED, item.getCount());
                break;
            case RETRIEVED:
            case RUNNING:
            case WARNING:
            case DOWNLOAD:
                runningItemsCount = runningItemsCount + item.getCount();
                break;
            case CANCELED:
            case CANCELING:
                cancelledItemCount = cancelledItemCount + item.getCount();
                break;
            default:
                throw new IllegalArgumentException("State " + item.getStatus() + "is not valid");
            }
        }
        statusTotalCountMap.put(Status.RUNNING, runningItemsCount);
        statusTotalCountMap.put(Status.CANCELLED, cancelledItemCount);
        return runningItemsCount + cancelledItemCount != 0;
    }

}
