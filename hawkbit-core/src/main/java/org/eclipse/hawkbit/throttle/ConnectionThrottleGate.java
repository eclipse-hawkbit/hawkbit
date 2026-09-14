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
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.context.AccessContext;

/**
 * Connection acquisition gate: throttles all tenant-scoped work (REST and schedulers) against the
 * tenant's fair share of the DB connection pool. Ties the acquired {@link Permit} to the connection
 * lifetime. Used by the DataSource decorator.
 * <p>
 * Virtual thread safe: tracks active connections by {@link Thread} identity (stable across carrier
 * thread changes during park/unpark). Handles reentrancy (nested {@code REQUIRES_NEW} transactions)
 * by acquiring a new connection from the pool but reusing the outer transaction's permit.
 */
public final class ConnectionThrottleGate {

    /** A source of raw JDBC connections (typically {@code DelegatingDataSource::getConnection}). */
    @FunctionalInterface
    public interface ConnectionSupplier {

        Connection get() throws SQLException;
    }

    private static final Map<Thread, ThrottledConnection> activeConnections = new ConcurrentHashMap<>();

    private ConnectionThrottleGate() {
    }

    /**
     * Acquire a connection, throttling it against the tenant's fair share. System (non-tenant) work
     * is exempt. Nested {@code REQUIRES_NEW} transactions acquire a new physical connection from the
     * pool but share the outer transaction's throttle permit (preventing self-deadlock).
     *
     * @param throttle the fair-share engine
     * @param timeout maximum wait for a permit before {@link ThrottledException}
     * @param raw supplier of the underlying pooled connection
     * @return the (possibly permit-bound) connection
     * @throws SQLException if the underlying connection cannot be opened
     * @throws ThrottledException if the tenant is over its share and no slot frees within the timeout
     */
    public static Connection acquire(final TenantThrottle throttle, final Duration timeout,
            final ConnectionSupplier raw) throws SQLException {
        final String tenant = AccessContext.tenant();
        if (tenant == null) {
            return raw.get(); // system work (no tenant context) → exempt
        }

        // Check for reentrancy: same thread acquiring again (REQUIRES_NEW nested transaction)
        final Thread currentThread = Thread.currentThread();
        final ThrottledConnection parent = activeConnections.get(currentThread);
        if (parent != null) {
            // Reentrant: get NEW connection from pool, NO new permit
            final Connection nestedConnection;
            try {
                nestedConnection = raw.get();
            } catch (final SQLException | RuntimeException e) {
                throw e; // no permit acquired, nothing to clean up
            }
            
            // Wrap nested connection, on close decrement parent's depth counter
            final ThrottledConnection nested = new ThrottledConnection(nestedConnection, parent::decrementDepth);
            parent.incrementDepth();
            return nested;
        }

        // First acquisition: acquire permit + connection
        final Permit permit = throttle.acquire(tenant, timeout);
        final Connection connection;
        try {
            connection = raw.get();
        } catch (final SQLException | RuntimeException e) {
            permit.close();
            throw e;
        }

        final ThrottledConnection wrapped = new ThrottledConnection(connection, () -> {
            activeConnections.remove(currentThread);
            permit.close();
        });
        activeConnections.put(currentThread, wrapped);
        return wrapped;
    }
}
