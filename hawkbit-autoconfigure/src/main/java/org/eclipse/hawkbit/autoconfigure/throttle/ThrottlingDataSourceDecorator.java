/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.throttle;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import javax.sql.DataSource;

import lombok.experimental.Delegate;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.throttle.Throttle;
import org.eclipse.hawkbit.throttle.Permit;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Layer-2 enforcement point: thin {@link DelegatingDataSource} that routes every connection acquisition throttling.
 * All logic (system keying, reentrancy, permit acquire/release tied to {@link Connection#close()}) lives in the gate and is unit-tested
 * in hawkbit-core; this class is only the Spring/JDBC glue.
 *
 * <p>The close / release of the connections is assumed to be always in the same thread
 */
class ThrottlingDataSourceDecorator extends DelegatingDataSource {

    // per instance - for every data source
    private final ThreadLocal<Permit> rootPermit = new ThreadLocal<>();

    private final Throttle throttle;
    private final Duration timeout;

    ThrottlingDataSourceDecorator(final DataSource targetDataSource, final Throttle throttle, final Duration timeout) {
        super(targetDataSource);
        this.throttle = throttle;
        this.timeout = timeout;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return acquire(throttle, timeout, super::getConnection);
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return acquire(throttle, timeout, () -> super.getConnection(username, password));
    }

    private Connection acquire(final Throttle throttle, final Duration timeout, final ConnectionSupplier raw) throws SQLException {
        final Permit root = rootPermit.get();
        final Permit permit;
        if (root == null) {
            // AccessContext.tenant() is null for internal work, which is exactly the throttle's key for it
            permit = throttle.acquire(AccessContext.tenant(), timeout);
            rootPermit.set(permit);
        } else {
            permit = root.acquire(timeout);
        }

        final Connection connection;
        try {
            connection = raw.get();
        } catch (final SQLException | RuntimeException e) {
            release(permit);
            throw e;
        }
        return new ThrottledConnection(connection, permit); // wrapper that will release permit when closed
    }

    private void release(final Permit permit) {
        try {
            permit.close();
        } finally {
            if (permit.isRootStale()) { // a root
                rootPermit.remove();
            }
        }
    }

    // A source of raw JDBC connections - supplier throwing exception
    @FunctionalInterface
    private interface ConnectionSupplier {

        Connection get() throws SQLException;
    }

    /**
     * JDBC {@link Connection} wrapper that releases a throttle {@link Permit} when closed. All methods except {@link #close()} are delegated
     * to the underlying connection via Lombok {@link Delegate} — generates ~100 forwarding methods at compile time with zero runtime overhead.
     */
    private final class ThrottledConnection implements Connection {

        @Delegate(excludes = Excludes.class) // don't delegate close() to the underlying connection
        private final Connection delegate;
        private final Permit permit;
        private boolean closed;

        ThrottledConnection(final Connection delegate, final Permit permit) {
            this.delegate = delegate;
            this.permit = permit;
        }

        @Override
        public synchronized void close() throws SQLException {
            if (closed) {
                return;
            }
            closed = true;
            try {
                delegate.close();
            } finally {
                release(permit);
            }
        }
    }

    private interface Excludes {

        void close() throws SQLException; // used to exclude Lombok {@link Delegate} handing of {@link #close()}
    }
}
