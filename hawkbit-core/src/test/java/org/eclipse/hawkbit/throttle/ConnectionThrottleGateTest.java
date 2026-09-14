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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.context.AccessContext.asTenant;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

class ConnectionThrottleGateTest {

    private static final ToIntFunction<String> UNLIMITED = tenant -> -1;

    @Test
    void systemThreadWithoutTenantContextIsExemptFromThrottling() throws Exception {
        final TenantThrottle throttle = new TenantThrottle(1, 0, UNLIMITED);
        final AtomicBoolean closed = new AtomicBoolean();

        // No tenant context → exempt
        final Connection connection = ConnectionThrottleGate.acquire(throttle, Duration.ZERO, () -> fakeConnection(closed));

        assertThat(throttle.inUse()).isZero(); // no permit taken
        connection.close();
        assertThat(closed).isTrue();
    }

    @Test
    void tenantWorkAcquiresPermitReleasedOnConnectionClose() {
        final TenantThrottle throttle = new TenantThrottle(2, 0, UNLIMITED);
        final AtomicBoolean closed = new AtomicBoolean();

        asTenant("acme", () -> {
            try {
                final Connection connection = ConnectionThrottleGate.acquire(throttle, Duration.ZERO, () -> fakeConnection(closed));
                assertThat(throttle.inUse("acme")).isEqualTo(1);

                connection.close();
                assertThat(closed).isTrue();
                assertThat(throttle.inUse("acme")).isZero();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void nestedConnectionOnSameThreadBypassesPermit() {
        final TenantThrottle throttle = new TenantThrottle(1, 0, UNLIMITED); // single slot

        asTenant("acme", () -> {
            try {
                final Connection first = ConnectionThrottleGate.acquire(throttle, Duration.ZERO, () -> fakeConnection(new AtomicBoolean()));
                assertThat(throttle.inUse("acme")).isEqualTo(1);

                // reentrant: would deadlock against the cap-1 pool if it tried to acquire a second permit
                final Connection nested = ConnectionThrottleGate.acquire(throttle, Duration.ZERO, () -> fakeConnection(new AtomicBoolean()));
                assertThat(throttle.inUse("acme")).isEqualTo(1);

                nested.close();
                assertThat(throttle.inUse("acme")).isEqualTo(1);
                first.close();
                assertThat(throttle.inUse("acme")).isZero();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void throttledTenantThrowsWithoutOpeningRawConnection() {
        final TenantThrottle throttle = new TenantThrottle(1, 0, UNLIMITED);
        throttle.acquire("filler", Duration.ZERO); // pool full (not via gate, different holder)
        final AtomicBoolean rawOpened = new AtomicBoolean();

        asTenant("acme", () -> assertThatExceptionOfType(ThrottledException.class).isThrownBy(() -> ConnectionThrottleGate.acquire(throttle,
                Duration.ZERO, () -> {
                    rawOpened.set(true);
                    return fakeConnection(new AtomicBoolean());
                })));

        assertThat(rawOpened).isFalse(); // never reached the pool
    }

    private static Connection fakeConnection(final AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
                ConnectionThrottleGateTest.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "close" -> {
                        closed.set(true);
                        yield null;
                    }
                    case "isClosed" -> closed.get();
                    case "toString" -> "fake-connection";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }
}
