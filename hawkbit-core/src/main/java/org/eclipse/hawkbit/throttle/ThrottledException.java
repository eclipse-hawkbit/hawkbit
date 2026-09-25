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

    /**
     * Why admission was refused. Low cardinality on purpose - it is meant to be a metric tag, so that the rejections
     * that mean "the resource is busy" can be told apart from the ones that mean "this deployment is misconfigured".
     */
    public enum Reason {

        /** The key is configured with no permits at all, so nothing it asks for can ever be admitted. */
        BLOCKED,
        /** No slot free and the caller asked not to wait. */
        LIMIT,
        /** No slot free and waiting is disabled for this key by configuration. */
        FAST_REJECT,
        /** A nested request that could not be admitted and could not be parked either - parking it would deadlock. */
        DEADLOCK,
        /** The key's own pending queue is full and this request is the one to drop. */
        KEY_QUEUE_FULL,
        /** The pending queue over all keys is full and this request is the one to drop. */
        QUEUE_FULL,
        /** Parked, then dropped to make room for another waiter. */
        EVICTED,
        /** Parked, but no slot became available within the timeout. */
        TIMEOUT
    }

    private final Reason reason;

    public ThrottledException(final Reason reason, final String message) {
        super(THIS_ERROR, message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
