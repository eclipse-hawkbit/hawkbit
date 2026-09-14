/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.throttle;

import java.sql.Connection;
import java.sql.SQLException;

import lombok.experimental.Delegate;

/**
 * JDBC {@link Connection} wrapper that releases a throttle {@link Permit} when closed. All methods
 * except {@link #close()} are delegated to the underlying connection via Lombok {@link Delegate} —
 * generates ~100 forwarding methods at compile time with zero runtime overhead.
 * <p>
 * Tracks reentrancy depth to handle nested {@code REQUIRES_NEW} transactions. Each nested transaction
 * holds a separate physical connection but shares the outer transaction's permit.
 */
final class ThrottledConnection implements Connection {

    private interface Excludes {
        void close() throws SQLException;
    }

    @Delegate(excludes = Excludes.class)
    private final Connection delegate;
    private final Runnable onClose;
    private int depth = 1;

    ThrottledConnection(final Connection delegate, final Runnable onClose) {
        this.delegate = delegate;
        this.onClose = onClose;
    }

    synchronized void incrementDepth() {
        depth++;
    }

    synchronized void decrementDepth() {
        depth--;
    }

    synchronized int getDepth() {
        return depth;
    }

    @Override
    public synchronized void close() throws SQLException {
        if (--depth == 0) {
            try {
                delegate.close();
            } finally {
                onClose.run();
            }
        }
    }
}
