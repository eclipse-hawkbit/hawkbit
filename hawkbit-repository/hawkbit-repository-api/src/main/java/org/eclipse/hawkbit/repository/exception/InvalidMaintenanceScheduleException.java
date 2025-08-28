/**
 * Copyright (c) 2018 Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * This exception is thrown if trying to set a maintenance schedule that is invalid. A maintenance schedule is considered to be valid only if
 * schedule, duration and timezone are all null, or are all valid; in which case there should be at least one valid window after the current
 * time.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvalidMaintenanceScheduleException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int durationErrorIndex;

    public InvalidMaintenanceScheduleException(final String message) {
        this(message, -1);
    }

    public InvalidMaintenanceScheduleException(final String message, final int errorIndex) {
        super(SpServerError.SP_MAINTENANCE_SCHEDULE_INVALID, message);
        this.durationErrorIndex = errorIndex;
    }

    public InvalidMaintenanceScheduleException(final String message, final Throwable cause) {
        this(message, cause, -1);
    }

    public InvalidMaintenanceScheduleException(final String message, final Throwable cause, final int errorIndex) {
        super(SpServerError.SP_MAINTENANCE_SCHEDULE_INVALID, message, cause);
        this.durationErrorIndex = errorIndex;
    }
}
