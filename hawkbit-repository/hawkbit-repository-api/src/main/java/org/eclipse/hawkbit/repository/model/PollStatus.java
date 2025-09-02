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

import java.time.LocalDateTime;

/**
 * The poll time object which holds all the necessary information around the target poll time, e.g. the last poll time, the next poll time and
 * the overdue poll time.
 */
public record PollStatus(LocalDateTime lastPollDate, LocalDateTime nextPollDate, LocalDateTime overdueDate, LocalDateTime currentDate) {

    /**
     * Calculates if the target poll time is overdue and the target has not been polled in the configured poll time interval.
     *
     * @return {@code true} if the current time is after the poll time overdue date otherwise {@code false}.
     */
    public boolean isOverdue() {
        return currentDate.isAfter(overdueDate);
    }
}