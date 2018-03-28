/**
 * Copyright (c) Siemens AG, 2018
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * This exception is thrown if trying to set a maintenance schedule that is
 * invalid. A maintenance schedule is considered to be valid only if schedule,
 * duration and timezone are all null, or are all valid; in which case there
 * should be at least one valid window after the current time.
 */
public class InvalidMaintenanceScheduleException extends AbstractServerRtException {
    private static final long serialVersionUID = 1L;

    private final int durationErrorIndex;

    /**
     * Constructor for {@link InvalidMaintenanceScheduleException}.
     *
     * @param message
     *            the message for this exception.
     */
    public InvalidMaintenanceScheduleException(final String message) {
        this(message, -1);
    }

    /**
     * Constructor for {@link InvalidMaintenanceScheduleException}.
     *
     * @param message
     *            the message for this exception.
     * @param errorIndex
     *            the error index of maintenance duration.
     */
    public InvalidMaintenanceScheduleException(final String message, final int errorIndex) {
        super(message, SpServerError.SP_MAINTENANCE_SCHEDULE_INVALID);
        this.durationErrorIndex = errorIndex;
    }

    /**
     * Constructor for {@link InvalidMaintenanceScheduleException}.
     *
     * @param message
     *            the message for this exception
     * @param cause
     *            the cause for this exception.
     */
    public InvalidMaintenanceScheduleException(final String message, final Throwable cause) {
        this(message, cause, -1);
    }

    /**
     * Constructor for {@link InvalidMaintenanceScheduleException}.
     *
     * @param message
     *            the message for this exception
     * @param cause
     *            the cause for this exception.
     * @param errorIndex
     *            the error index of maintenance duration.
     */
    public InvalidMaintenanceScheduleException(final String message, final Throwable cause, final int errorIndex) {
        super(message, SpServerError.SP_MAINTENANCE_SCHEDULE_INVALID, cause);
        this.durationErrorIndex = errorIndex;
    }

    /**
     * Get the error index position for maintenance window duration.
     *
     * @return error index.
     */
    public int getDurationErrorIndex() {
        return durationErrorIndex;
    }
}
