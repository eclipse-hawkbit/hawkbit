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
 * Store target count of a {@link Rollout} or {@link RolloutGroup} for every
 * {@link Status}.
 *
 */
public class TotalTargetCountStatus {

    /**
     * Status of the total target counts.
     */
    public enum Status {
        /**
         * Action is scheduled.
         */
        SCHEDULED,

        /**
         * Action is still running.
         */
        RUNNING,

        /**
         * Action failed.
         */
        ERROR,

        /**
         * Action is completed.
         */
        FINISHED,

        /**
         * Action is canceled.
         */
        CANCELLED,

        /**
         * Action is not started yet.
         */
        NOTSTARTED
    }

    private final Map<Status, Long> statusTotalCountMap = new EnumMap<>(Status.class);
    private final Long totalTargetCount;
    private final Action.ActionType rolloutType;

    /**
     * Create a new states map with the target count for each state.
     *
     * @param targetCountActionStatus
     *            the action state map
     * @param totalTargetCount
     *            the total target count
     * @param rolloutType
     *            the type of the rollout
     */
    public TotalTargetCountStatus(final List<TotalTargetCountActionStatus> targetCountActionStatus,
            final Long totalTargetCount, final Action.ActionType rolloutType) {
        this.totalTargetCount = totalTargetCount;
        this.rolloutType = rolloutType;
        addToTotalCount(targetCountActionStatus);
    }

    /**
     * Create a new states map with the target count for each state.
     *
     * @param totalTargetCount
     *            the total target count
     * @param rolloutType
     *            the type of the rollout
     */
    public TotalTargetCountStatus(final Long totalTargetCount, final Action.ActionType rolloutType) {
        this(Collections.emptyList(), totalTargetCount, rolloutType);
    }

    /**
     * The current state mape which the total target count
     *
     * @return the statusTotalCountMap the state map
     */
    public Map<Status, Long> getStatusTotalCountMap() {
        return statusTotalCountMap;
    }

    /**
     * Gets the total target count from a state.
     *
     * @param status
     *            the state key
     * @return the current target count cannot be <null>
     */
    public Long getTotalTargetCountByStatus(final Status status) {
        final Long count = statusTotalCountMap.get(status);
        return count == null ? 0L : count;
    }

    /**
     * @return finished percentage of targets
     */
    public float getFinishedPercent() {
        return ((float) getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED) / totalTargetCount) * 100;
    }

    /**
     * Populate all target status to a the given map
     *
     * @param statusTotalCountMap
     *            the map
     * @param rolloutStatusCountItems
     *            all target {@link Status} with total count
     */
    private void addToTotalCount(final List<TotalTargetCountActionStatus> targetCountActionStatus) {
        if (targetCountActionStatus == null) {
            statusTotalCountMap.put(TotalTargetCountStatus.Status.NOTSTARTED, totalTargetCount);
            return;
        }
        statusTotalCountMap.put(Status.RUNNING, 0L);
        Long notStartedTargetCount = totalTargetCount;
        for (final TotalTargetCountActionStatus item : targetCountActionStatus) {
            addToTotalCount(item);
            notStartedTargetCount -= item.getCount();
        }
        statusTotalCountMap.put(TotalTargetCountStatus.Status.NOTSTARTED, notStartedTargetCount);
    }

    private void addToTotalCount(final TotalTargetCountActionStatus item) {
        final Status status = convertStatus(item.getStatus());
        statusTotalCountMap.merge(status, item.getCount(), Long::sum);
    }

    // Exception squid:MethodCyclomaticComplexity - simple state conversion, not
    // really complex.
    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    private Status convertStatus(final Action.Status status) {
        switch (status) {
        case SCHEDULED:
            return Status.SCHEDULED;
        case ERROR:
            return Status.ERROR;
        case FINISHED:
            return Status.FINISHED;
        case CANCELED:
            return Status.CANCELLED;
        case RETRIEVED:
        case RUNNING:
        case WARNING:
        case DOWNLOAD:
        case WAIT_FOR_CONFIRMATION:
        case CANCELING:
            return Status.RUNNING;
        case DOWNLOADED:
            return Action.ActionType.DOWNLOAD_ONLY == rolloutType ? Status.FINISHED : Status.RUNNING;
        default:
            throw new IllegalArgumentException("State " + status + "is not valid");
        }
    }
}
