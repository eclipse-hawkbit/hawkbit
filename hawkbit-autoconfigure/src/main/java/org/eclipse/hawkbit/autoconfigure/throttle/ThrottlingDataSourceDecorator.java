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

import static org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration.TENANT_TAG;
import static org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration.TENANT_TAG_VALUE_PROVIDER;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.sql.DataSource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.experimental.Delegate;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.throttle.Throttle;
import org.eclipse.hawkbit.throttle.ThrottledException;
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

    // how long a caller waited to actually get a connection - the throttle queue plus the pool itself, which is the
    // latency the request pays. Only successful acquisitions are recorded; refusals are counted by REJECTED_METER
    private static final String ACQUIRE_METER = "hawkbit.throttle.connections.acquire";
    // refused acquisitions, split by why - see ThrottledException.Reason
    private static final String REJECTED_METER = "hawkbit.throttle.connections.rejected";
    private static final String REASON_TAG = "reason";
    // how long a connection was held, borrow to close - hikaricp.connections.usage split per tenant, shows who holds the pool
    private static final String USAGE_METER = "hawkbit.throttle.connections.usage";
    private static final String PRIORITY_TAG = "priority";
    // throttle state, evaluated on scrape only - with the throttle in front, hikaricp.connections.pending stays ~0 by design
    private static final String ACTIVE_METER = "hawkbit.throttle.connections.active"; // permits mirror connections 1:1
    private static final String PENDING_METER = "hawkbit.throttle.connections.pending";

    // per instance - for every data source
    private final ThreadLocal<Permit> rootPermit = new ThreadLocal<>();

    private final Throttle throttle;
    private final Duration timeout;
    private final Supplier<MeterRegistry> meterRegistrySupplier;

    // the registry is resolved on first use, not injected: this data source is built by a BeanPostProcessor, so at
    // construction time the metrics infrastructure does not exist yet. null until then, and metrics are simply skipped
    private volatile MeterRegistry meterRegistry;

    ThrottlingDataSourceDecorator(final DataSource targetDataSource, final Throttle throttle, final Duration timeout) {
        this(targetDataSource, throttle, timeout, () -> null);
    }

    ThrottlingDataSourceDecorator(
            final DataSource targetDataSource, final Throttle throttle, final Duration timeout,
            final Supplier<MeterRegistry> meterRegistrySupplier) {
        super(targetDataSource);
        this.throttle = throttle;
        this.timeout = timeout;
        this.meterRegistrySupplier = meterRegistrySupplier;
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
        final long startNano = System.nanoTime();
        final Permit root = rootPermit.get();
        final Permit permit;
        try {
            if (root == null) {
                // AccessContext.tenant() is null for internal work, which is exactly the throttle's key for it
                permit = throttle.acquire(AccessContext.tenant(), timeout);
                rootPermit.set(permit);
            } else {
                permit = root.acquire(timeout);
            }
        } catch (final ThrottledException e) {
            countRejected(e);
            throw e;
        }

        final Connection connection;
        try {
            connection = raw.get();
        } catch (final SQLException | RuntimeException e) {
            release(permit);
            throw e;
        }
        recordConnectionTime(System.nanoTime() - startNano);
        return new ThrottledConnection(connection, permit); // wrapper that will release permit when closed
    }

    private void countRejected(final ThrottledException e) {
        final MeterRegistry registry = meterRegistry();
        if (registry != null) {
            registry.counter(REJECTED_METER, TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get(), REASON_TAG, e.getReason().name(), PRIORITY_TAG, priority()).increment();
        }
    }

    private void recordConnectionTime(final long nanos) {
        final MeterRegistry registry = meterRegistry();
        if (registry != null) {
            registry.timer(ACQUIRE_METER, TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get()).record(nanos, TimeUnit.NANOSECONDS);
        }
    }

    private void recordUsageTime(final String tenant, final long nanos) {
        final MeterRegistry registry = meterRegistry();
        if (registry != null) {
            registry.timer(USAGE_METER, TENANT_TAG, tenant, PRIORITY_TAG, priority()).record(nanos, TimeUnit.NANOSECONDS);
        }
    }

    // as declared by the thread - close runs on the borrowing thread, see class doc
    private static String priority() {
        return Throttle.isHighPriorityThread() ? "high" : "normal";
    }

    // resolved once, on the first acquisition after the registry bean exists; before that every call re-asks, which
    // is a map lookup in the bean factory and only happens while the context is still coming up
    private MeterRegistry meterRegistry() {
        MeterRegistry registry = meterRegistry;
        if (registry == null) {
            registry = meterRegistrySupplier.get();
            if (registry != null) {
                registerGauges(registry);
            }
            meterRegistry = registry;
        }
        return registry;
    }

    private void registerGauges(final MeterRegistry registry) {
        Gauge.builder(ACTIVE_METER, throttle, t -> t.stats().highGranted()).tag(PRIORITY_TAG, "high").register(registry);
        Gauge.builder(ACTIVE_METER, throttle, t -> {
            final Throttle.Stats stats = t.stats();
            return stats.globalGranted() - stats.highGranted();
        }).tag(PRIORITY_TAG, "normal").register(registry);
        Gauge.builder(PENDING_METER, throttle, t -> t.stats().pending()).register(registry);
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
        private final String tenant = TENANT_TAG_VALUE_PROVIDER.get(); // captured at borrow - the tenant context may differ at close
        private final long borrowNano = System.nanoTime();
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
                recordUsageTime(tenant, System.nanoTime() - borrowNano);
            }
        }
    }

    private interface Excludes {

        void close() throws SQLException; // used to exclude Lombok {@link Delegate} handing of {@link #close()}
    }
}
