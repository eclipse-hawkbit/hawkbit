/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.throttle;

import java.io.Serial;
import java.time.Duration;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown by {@link Throttle#acquire(String, Duration)} or {@link Permit#acquire(Duration)} when no slot became available within the requested
 * timeout, or when a reentrant request was refused because parking it would have deadlocked every permit holder.
 * Mapped to HTTP 429 at the REST layer.
 */
public class ThrottledException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_THROTTLED;

    public ThrottledException(final String message) {
        super(THIS_ERROR, message);
    }
}
