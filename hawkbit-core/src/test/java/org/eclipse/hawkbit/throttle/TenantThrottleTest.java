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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

class TenantThrottleTest {

    private static final ToIntFunction<String> UNLIMITED = tenant -> -1;

    @Test
    void grantsPermitAndTracksInUseOnIdleEngine() {
        final TenantThrottle throttle = new TenantThrottle(2, 0, UNLIMITED);

        final Permit permit = throttle.acquire("A", Duration.ZERO);
        assertThat(permit).isNotNull();
        assertThat(throttle.inUse()).isEqualTo(1);
        assertThat(throttle.inUse("A")).isEqualTo(1);
        permit.close();

        assertThat(throttle.inUse()).isZero();
        assertThat(throttle.inUse("A")).isZero();
    }

    @Test
    void rejectsOverShareImmediatelyWhenTimeoutZero() {
        // threshold 0 => always contended (no burst); tenant A hard-limited to a single slot
        final TenantThrottle throttle = new TenantThrottle(2, 0, tenant -> "A".equals(tenant) ? 1 : -1);

        final Permit first = throttle.acquire("A", Duration.ZERO);
        assertThat(first).isNotNull();

        assertThatExceptionOfType(ThrottledException.class)
                .isThrownBy(() -> throttle.acquire("A", Duration.ZERO));

        first.close();
        assertThat(throttle.inUse()).isZero();
    }

    @Test
    void dynamicShareShrinksAsMoreTenantsCompete() {
        // cap 4, threshold 0 => always contended; both tenants unlimited per-tenant
        final TenantThrottle throttle = new TenantThrottle(4, 0, UNLIMITED);

        throttle.acquire("A", Duration.ZERO); // A=1, only A active => share 4
        throttle.acquire("B", Duration.ZERO); // B active => share now ceil(4/2)=2
        throttle.acquire("A", Duration.ZERO); // A=2, still < share 2? 1<2 ok => A=2

        assertThat(throttle.inUse("A")).isEqualTo(2);
        // A now at its fair share of 2 while B competes => rejected
        assertThatExceptionOfType(ThrottledException.class)
                .isThrownBy(() -> throttle.acquire("A", Duration.ZERO));
    }

    @Test
    void unlimitedTenantIsBoundedOnlyByGlobalCapacity() {
        // cap 4, threshold 0 => always contended; single tenant opted out (-1)
        final TenantThrottle throttle = new TenantThrottle(4, 0, UNLIMITED);

        for (int i = 0; i < 4; i++) {
            assertThat(throttle.acquire("U", Duration.ZERO)).isNotNull();
        }
        assertThat(throttle.inUse("U")).isEqualTo(4);

        // pool full => even an unlimited tenant is rejected
        assertThatExceptionOfType(ThrottledException.class)
                .isThrownBy(() -> throttle.acquire("U", Duration.ZERO));
    }

    @Test
    void blockedAcquireIsGrantedWhenAnotherPermitIsReleased() throws Exception {
        // cap 1, threshold 0 => single slot, always contended
        final TenantThrottle throttle = new TenantThrottle(1, 0, UNLIMITED);
        final Permit held = throttle.acquire("A", Duration.ZERO);
        assertThat(throttle.inUse()).isEqualTo(1);

        final CountDownLatch started = new CountDownLatch(1);
        final AtomicReference<Permit> granted = new AtomicReference<>();
        final Thread waiter = new Thread(() -> {
            started.countDown();
            granted.set(throttle.acquire("B", Duration.ofSeconds(5)));
        });
        waiter.start();
        started.await();
        Thread.sleep(200); // let B block on the full pool
        assertThat(granted.get()).isNull();

        held.close(); // frees the only slot -> must wake B
        waiter.join(2000);

        assertThat(granted.get()).isNotNull();
        assertThat(throttle.inUse("B")).isEqualTo(1);
    }

    @Test
    void blockedAcquireThrowsAfterTimeout() {
        final TenantThrottle throttle = new TenantThrottle(1, 0, UNLIMITED);
        throttle.acquire("A", Duration.ZERO); // takes the only slot, never released

        final long start = System.nanoTime();
        assertThatExceptionOfType(ThrottledException.class)
                .isThrownBy(() -> throttle.acquire("B", Duration.ofMillis(200)));
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isGreaterThanOrEqualTo(150); // actually waited for the deadline
    }

    @Test
    void grantsWaitersInArrivalOrderWakingExactlyOne() throws Exception {
        final TenantThrottle throttle = new TenantThrottle(1, 0, UNLIMITED);
        final Permit held = throttle.acquire("X", Duration.ZERO); // holds the only slot

        final AtomicReference<Permit> aPermit = new AtomicReference<>();
        final AtomicReference<Permit> bPermit = new AtomicReference<>();
        final CountDownLatch aDone = new CountDownLatch(1);
        final CountDownLatch bDone = new CountDownLatch(1);

        final Thread a = new Thread(() -> {
            aPermit.set(throttle.acquire("A", Duration.ofSeconds(5)));
            aDone.countDown();
        });
        a.start();
        Thread.sleep(150); // A parks first
        final Thread b = new Thread(() -> {
            bPermit.set(throttle.acquire("B", Duration.ofSeconds(5)));
            bDone.countDown();
        });
        b.start();
        Thread.sleep(150); // B parks second

        held.close(); // frees exactly one slot

        assertThat(aDone.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(aPermit.get()).isNotNull(); // A granted (arrived first)
        // wake-exactly-one: the single freed slot must not have also woken B
        assertThat(bDone.await(200, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(bPermit.get()).isNull();

        aPermit.get().close(); // now B gets the slot
        assertThat(bDone.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(bPermit.get()).isNotNull();
    }
}
