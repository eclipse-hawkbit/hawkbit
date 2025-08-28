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

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Store target count of a {@link Rollout} or {@link RolloutGroup} for every {@link Status}.
 */
public class TotalTargetCountStatus {

    private final Map<Status, Long> statusTotalCountMap = new EnumMap<>(Status.class);
    private final Long totalTargetCount;
    private final Action.ActionType rolloutType;

    /**
     * Create a new states map with the target count for each state.
     *
     * @param targetCountActionStatus the action state map
     * @param totalTargetCount the total target count
     * @param rolloutType the type of the rollout
     */
    public TotalTargetCountStatus(
            final List<TotalTargetCountActionStatus> targetCountActionStatus,
            final Long totalTargetCount,
            final Action.ActionType rolloutType) {
        this.totalTargetCount = totalTargetCount;
        this.rolloutType = rolloutType;
        addToTotalCount(targetCountActionStatus);
    }

    /**
     * Create a new states map with the target count for each state.
     *
     * @param totalTargetCount the total target count
     * @param rolloutType the type of the rollout
     */
    public TotalTargetCountStatus(final Long totalTargetCount, final Action.ActionType rolloutType) {
        this(Collections.emptyList(), totalTargetCount, rolloutType);
    }

    /**
     * Gets the total target count from a state.
     *
     * @param status the state key
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

    // Exception squid:MethodCyclomaticComplexity - simple state conversion, not really complex.
    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    private Status convertStatus(final Action.Status status) {
        return switch (status) {
            case SCHEDULED -> Status.SCHEDULED;
            case ERROR -> Status.ERROR;
            case FINISHED -> Status.FINISHED;
            case CANCELED -> Status.CANCELLED;
            case RETRIEVED, RUNNING, WARNING, DOWNLOAD, WAIT_FOR_CONFIRMATION, CANCELING -> Status.RUNNING;
            case DOWNLOADED -> Action.ActionType.DOWNLOAD_ONLY == rolloutType ? Status.FINISHED : Status.RUNNING;
            default -> throw new IllegalArgumentException("State " + status + "is not valid");
        };
    }

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
}
