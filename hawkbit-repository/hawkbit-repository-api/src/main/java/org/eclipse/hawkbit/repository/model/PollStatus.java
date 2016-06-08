/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.time.LocalDateTime;

/**
 * The poll time object which holds all the necessary information around the
 * target poll time, e.g. the last poll time, the next poll time and the overdue
 * poll time.
 *
 */
public class PollStatus {
    private final LocalDateTime lastPollDate;
    private final LocalDateTime nextPollDate;
    private final LocalDateTime overdueDate;
    private final LocalDateTime currentDate;

    public PollStatus(final LocalDateTime lastPollDate, final LocalDateTime nextPollDate,
            final LocalDateTime overdueDate, final LocalDateTime currentDate) {
        this.lastPollDate = lastPollDate;
        this.nextPollDate = nextPollDate;
        this.overdueDate = overdueDate;
        this.currentDate = currentDate;
    }

    /**
     * calculates if the target poll time is overdue and the target has not been
     * polled in the configured poll time interval.
     *
     * @return {@code true} if the current time is after the poll time overdue
     *         date otherwise {@code false}.
     */
    public boolean isOverdue() {
        return currentDate.isAfter(overdueDate);
    }

    /**
     * @return the lastPollDate
     */
    public LocalDateTime getLastPollDate() {
        return lastPollDate;
    }

    public LocalDateTime getNextPollDate() {
        return nextPollDate;
    }

    public LocalDateTime getOverdueDate() {
        return overdueDate;
    }

    public LocalDateTime getCurrentDate() {
        return currentDate;
    }

    @Override
    public String toString() {
        return "PollTime [lastPollDate=" + lastPollDate + ", nextPollDate=" + nextPollDate + ", overdueDate="
                + overdueDate + ", currentDate=" + currentDate + "]";
    }
}
