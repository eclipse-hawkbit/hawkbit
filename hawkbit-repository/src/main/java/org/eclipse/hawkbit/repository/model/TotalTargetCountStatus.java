/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Store all states with the target count of a rollout or rolloutgroup.
 *
 */
public class TotalTargetCountStatus {

    /**
     * Status of the total target counts.
     */
    public enum Status {
        SCHEDULED, RUNNING, ERROR, FINISHED, CANCELLED, NOTSTARTED
    }

    private final Map<Status, Long> statusTotalCountMap = new EnumMap<>(Status.class);
    private final Long totalTargetCount;

    /**
     * Create a new states map with the target count for each state.
     *
     * @param targetCountActionStatus
     *            the action state map
     * @param totalTargets
     *            the total target count
     */
    public TotalTargetCountStatus(final List<TotalTargetCountActionStatus> targetCountActionStatus,
            final Long totalTargetCount) {
        this.totalTargetCount = totalTargetCount;
        mapActionStatusToTotalTargetCountStatus(targetCountActionStatus);
    }

    /**
     * Create a new states map with the target count for each state.
     *
     * @param totalTargetCount
     *            the total target count
     */
    public TotalTargetCountStatus(final Long totalTargetCount) {
        this(Collections.emptyList(), totalTargetCount);
    }

    /**
     * The current state mape which the total target count
     *
     * @return the statusTotalCountMap the state map
     */
    public Map<Status, Long> getStatusTotalCountMap() {
        return this.statusTotalCountMap;
    }

    /**
     * Gets the total target count from a state.
     *
     * @param status
     *            the state key
     * @return the current target count cannot be <null>
     */
    public Long getTotalTargetCountByStatus(final Status status) {
        final Long count = this.statusTotalCountMap.get(status);
        return count == null ? 0L : count;
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
    private final void mapActionStatusToTotalTargetCountStatus(
            final List<TotalTargetCountActionStatus> targetCountActionStatus) {
        if (targetCountActionStatus == null) {
            this.statusTotalCountMap.put(TotalTargetCountStatus.Status.NOTSTARTED, this.totalTargetCount);
            return;
        }
        this.statusTotalCountMap.put(Status.RUNNING, 0L);
        Long notStartedTargetCount = this.totalTargetCount;
        for (final TotalTargetCountActionStatus item : targetCountActionStatus) {
            switch (item.getStatus()) {
            case SCHEDULED:
                this.statusTotalCountMap.put(Status.SCHEDULED, item.getCount());
                break;
            case ERROR:
                this.statusTotalCountMap.put(Status.ERROR, item.getCount());
                break;
            case FINISHED:
                this.statusTotalCountMap.put(Status.FINISHED, item.getCount());
                break;
            case RETRIEVED:
            case RUNNING:
            case WARNING:
            case DOWNLOAD:
            case CANCELING:
                final Long runningItemsCount = this.statusTotalCountMap.get(Status.RUNNING) + item.getCount();
                this.statusTotalCountMap.put(Status.RUNNING, runningItemsCount);
                break;
            case CANCELED:
                this.statusTotalCountMap.put(Status.CANCELLED, item.getCount());
                break;
            default:
                throw new IllegalArgumentException("State " + item.getStatus() + "is not valid");
            }
            notStartedTargetCount -= item.getCount();
        }
        this.statusTotalCountMap.put(TotalTargetCountStatus.Status.NOTSTARTED, notStartedTargetCount);
    }

}
