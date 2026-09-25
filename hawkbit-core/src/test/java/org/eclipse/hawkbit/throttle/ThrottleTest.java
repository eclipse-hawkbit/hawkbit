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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.hawkbit.throttle.Config.Policy;
import org.eclipse.hawkbit.throttle.Config.Priority;
import org.junit.jupiter.api.Test;

class ThrottleTest {

    // long enough that a parked waiter is still parked when the assertions below run
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final Duration SETTLE = Duration.ofMillis(150);

    // --- basic accounting ---

    @Test
    void grantsPermitAndTracksUsageOnIdleEngine() {
        final Throttle throttle = throttle(2);

        final Permit permit = throttle.acquire("A", Duration.ZERO);
        assertThat(permit).isNotNull();
        assertThat(throttle.stats().inUse()).isEqualTo(1);
        assertThat(throttle.stats().inUse("A")).isEqualTo(1);
        assertThat(throttle.stats().units()).isEqualTo(1);

        permit.close();
        assertThat(throttle.stats().inUse()).isZero();
        assertThat(throttle.stats().inUse("A")).isZero();
        assertThat(throttle.stats().units()).isZero();
    }

    @Test
    void closeIsIdempotent() {
        final Throttle throttle = throttle(2);
        final Permit permit = throttle.acquire("A", Duration.ZERO);

        permit.close();
        permit.close(); // must not double-release

        assertThat(throttle.stats().inUse()).isZero();
        assertThat(throttle.stats().units()).isZero();
    }

    // --- ceilings ---

    @Test
    void rejectsOverCeilingImmediatelyWhenTimeoutZero() {
        final Throttle throttle = throttle(2, config -> config.getTenants().put("A", 1));

        final Permit first = throttle.acquire("A", Duration.ZERO);
        assertThat(first).isNotNull();

        assertThatExceptionOfType(ThrottledException.class).isThrownBy(() -> throttle.acquire("A", Duration.ZERO));

        first.close();
        assertThat(throttle.stats().inUse()).isZero();
    }

    @Test
    void cappedKeyIsBoundedByItsLimitWhileOthersAreNot() {
        // the point of a per-tenant override: one key is capped, every other key stays bounded only by the pool
        final Throttle throttle = throttle(4, config -> config.getTenants().put("A", 2));

        throttle.acquire("A", Duration.ZERO);
        throttle.acquire("A", Duration.ZERO);
        assertThat(throttle.stats().inUse("A")).isEqualTo(2); // at its ceiling

        assertThatExceptionOfType(ThrottledException.class).isThrownBy(() -> throttle.acquire("A", Duration.ZERO));

        // B opted out of any ceiling, so A's cap does not constrain it — it takes the rest of the pool
        throttle.acquire("B", Duration.ZERO);
        throttle.acquire("B", Duration.ZERO);
        assertThat(throttle.stats().inUse("B")).isEqualTo(2);
        assertThat(throttle.stats().inUse()).isEqualTo(4);
    }

    @Test
    void tenantLimitMatchesConfiguredKeyRegardlessOfCase() {
        // the configured key need not match the casing of the tenant id arriving from the security context
        final Throttle throttle = throttle(4, config -> config.getTenants().put("acme", 1));

        assertThat(throttle.acquire("ACME", Duration.ZERO)).isNotNull();
        assertThatExceptionOfType(ThrottledException.class).isThrownBy(() -> throttle.acquire("ACME", Duration.ZERO));
    }

    @Test
    void unlimitedKeyIsStillBoundedByGlobalCapacity() {
        final Throttle throttle = throttle(4); // limit defaults to -1 = unlimited

        for (int i = 0; i < 4; i++) {
            assertThat(throttle.acquire("U", Duration.ZERO)).isNotNull();
        }
        assertThat(throttle.stats().inUse("U")).isEqualTo(4);

        assertThatExceptionOfType(ThrottledException.class).isThrownBy(() -> throttle.acquire("U", Duration.ZERO));
    }

    // --- waiting ---

    @Test
    void blockedAcquireIsGrantedWhenAnotherPermitIsReleased() throws Exception {
        final Throttle throttle = throttle(1);
        final Permit held = throttle.acquire("A", Duration.ZERO);

        final AtomicReference<Permit> granted = new AtomicReference<>();
        final Thread waiter = new Thread(() -> granted.set(throttle.acquire("B", WAIT)));
        waiter.start();
        settle();
        assertThat(granted.get()).isNull();

        held.close(); // frees the only slot -> must wake B
        waiter.join(WAIT.toMillis());

        assertThat(granted.get()).isNotNull();
        assertThat(throttle.stats().inUse("B")).isEqualTo(1);
    }

    @Test
    void blockedAcquireThrowsAfterTimeout() {
        final Throttle throttle = throttle(1);
        throttle.acquire("A", Duration.ZERO); // takes the only slot, never released

        final long start = System.nanoTime();
        assertThatExceptionOfType(ThrottledException.class).isThrownBy(() -> throttle.acquire("B", Duration.ofMillis(200)));

        assertThat((System.nanoTime() - start) / 1_000_000).isGreaterThanOrEqualTo(150); // actually waited
    }

    @Test
    void grantsWaitersInArrivalOrderWakingExactlyOne() throws Exception {
        final Throttle throttle = throttle(1);
        final Permit held = throttle.acquire("X", Duration.ZERO);

        final AtomicReference<Permit> aPermit = new AtomicReference<>();
        final AtomicReference<Permit> bPermit = new AtomicReference<>();
        final CountDownLatch aDone = new CountDownLatch(1);
        final CountDownLatch bDone = new CountDownLatch(1);

        new Thread(() -> {
            aPermit.set(throttle.acquire("A", WAIT));
            aDone.countDown();
        }).start();
        settle(); // A parks first
        new Thread(() -> {
            bPermit.set(throttle.acquire("B", WAIT));
            bDone.countDown();
        }).start();
        settle(); // B parks second

        held.close(); // frees exactly one slot

        assertThat(aDone.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(aPermit.get()).isNotNull(); // A granted, it arrived first
        // wake-exactly-one: the single freed slot must not have also woken B
        assertThat(bDone.await(200, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(bPermit.get()).isNull();

        aPermit.get().close();
        assertThat(bDone.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(bPermit.get()).isNotNull();
    }

    @Test
    void serviceRotatesAcrossKeysInsteadOfDrainingOneBacklog() throws Exception {
        // cap 4 held entirely by X. A queues two waiters, B one. No key has a ceiling, so A stays eligible for a
        // second grant — only rotation stops it being served twice before B is looked at.
        final Throttle throttle = throttle(4);
        final List<Permit> held = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            held.add(throttle.acquire("X", Duration.ZERO));
        }

        final List<String> servedOrder = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch allServed = new CountDownLatch(3);
        for (final String label : List.of("A1", "A2", "B1")) {
            waiter(throttle, label.substring(0, 1), label, servedOrder, allServed).start();
            settle(); // deterministic arrival order
        }

        for (int i = 0; i < 3; i++) {
            held.get(i).close();
            settle(); // let the woken waiter settle before freeing the next slot
        }

        assertThat(allServed.await(2, TimeUnit.SECONDS)).isTrue();
        // without rotation this would be A1, A2, B1 — A drains its backlog before B is reached
        assertThat(servedOrder).containsExactly("A1", "B1", "A2");
    }

    private static Thread waiter(
            final Throttle throttle, final String key, final String label, final List<String> servedOrder, final CountDownLatch done) {
        return new Thread(() -> {
            throttle.acquire(key, WAIT);
            servedOrder.add(label);
            done.countDown();
        }, label);
    }

    // --- priority floor for internal work (the null key) ---

    @Test
    void internalWorkIsServedAheadOfWaitingTenantsUpToItsFloor() throws Exception {
        // cap 2, floor of 1 slot for the null key
        final Throttle throttle = throttle(2, config -> config.setSystemFloor(1));
        final Permit t1 = throttle.acquire("acme", Duration.ZERO);
        final Permit t2 = throttle.acquire("acme", Duration.ZERO);
        assertThat(throttle.stats().inUse()).isEqualTo(2); // full

        final CountDownLatch tenantDone = new CountDownLatch(1);
        final CountDownLatch systemDone = new CountDownLatch(1);

        new Thread(() -> {
            throttle.acquire("other", WAIT);
            tenantDone.countDown();
        }).start();
        settle(); // tenant parks FIRST
        new Thread(() -> {
            throttle.acquire(null, WAIT);
            systemDone.countDown();
        }).start();
        settle(); // internal work parks SECOND

        t1.close(); // one slot freed

        // despite arriving later, internal work wins while below its floor
        assertThat(systemDone.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(tenantDone.await(200, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(throttle.stats().inUse(null)).isEqualTo(1);

        t2.close();
        assertThat(tenantDone.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void internalWorkLosesPriorityAboveItsFloor() throws Exception {
        // cap 4, floor of 1 slot, which internal work already holds, so it queues behind the earlier tenant
        final Throttle throttle = throttle(4, config -> config.setSystemFloor(1));
        final Permit systemHeld = throttle.acquire(null, Duration.ZERO);
        final List<Permit> filler = new ArrayList<>();
        for (final String key : List.of("acme", "bosch", "ciena")) {
            filler.add(throttle.acquire(key, Duration.ZERO));
        }
        assertThat(throttle.stats().inUse()).isEqualTo(4); // full

        final CountDownLatch tenantDone = new CountDownLatch(1);
        final CountDownLatch systemDone = new CountDownLatch(1);

        new Thread(() -> {
            throttle.acquire("other", WAIT);
            tenantDone.countDown();
        }).start();
        settle(); // tenant parks first
        new Thread(() -> {
            throttle.acquire(null, WAIT);
            systemDone.countDown();
        }).start();
        settle();

        filler.get(0).close(); // one slot freed

        // internal work is at its floor => no priority, service order applies and the tenant wins
        assertThat(tenantDone.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(systemDone.await(200, TimeUnit.MILLISECONDS)).isFalse();

        systemHeld.close();
        assertThat(systemDone.await(2, TimeUnit.SECONDS)).isTrue();
    }

    // --- parent / child permits ---

    @Test
    void childrenTakeTheirOwnPermitsWithinOneUnitOfWork() {
        final Throttle throttle = throttle(3);

        final Permit root = throttle.acquire("acme", Duration.ZERO);
        final Permit child = root.acquire(Duration.ZERO);
        final Permit grandchild = child.acquire(Duration.ZERO);

        // every permit counted, so permits mirror real resource usage...
        assertThat(throttle.stats().inUse()).isEqualTo(3);
        assertThat(throttle.stats().inUse("acme")).isEqualTo(3); // children inherit the root's key
        // ...but nesting of any depth flattens onto one unit of work
        assertThat(throttle.stats().units()).isEqualTo(1);

        grandchild.close();
        child.close();
        assertThat(throttle.stats().inUse()).isEqualTo(1);
        assertThat(throttle.stats().units()).isEqualTo(1);

        root.close();
        assertThat(throttle.stats().units()).isZero();
    }

    @Test
    void closingParentLeavesOpenChildValid() {
        final Throttle throttle = throttle(2);
        final Permit parent = throttle.acquire("acme", Duration.ZERO);
        final Permit child = parent.acquire(Duration.ZERO);

        parent.close(); // independent lifetimes: releases only its own slot

        assertThat(throttle.stats().inUse()).isEqualTo(1);
        assertThat(throttle.stats().units()).isEqualTo(1); // unit stays alive while the child holds a permit

        child.close();
        assertThat(throttle.stats().inUse()).isZero();
        assertThat(throttle.stats().units()).isZero();
    }

    @Test
    void acquiringFromAClosedPermitIsRejected() {
        final Throttle throttle = throttle(2);
        final Permit parent = throttle.acquire("acme", Duration.ZERO);
        parent.close();

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> parent.acquire(Duration.ZERO));
    }

    @Test
    void childBypassesCeiling() {
        // ceiling of 1 for every key, but a unit of work already inside must be able to finish
        final Throttle throttle = throttle(4, config -> config.setLimit(1));

        final Permit parent = throttle.acquire("acme", Duration.ZERO);
        assertThat(throttle.stats().inUse("acme")).isEqualTo(1); // at its ceiling

        // a fresh unit of work is refused...
        assertThatExceptionOfType(ThrottledException.class).isThrownBy(() -> throttle.acquire("acme", Duration.ZERO));
        // ...but the existing one is admitted, since blocking it would pin the resource
        assertThat(parent.acquire(Duration.ZERO)).isNotNull();
        assertThat(throttle.stats().inUse("acme")).isEqualTo(2);
    }

    @Test
    void childIsRefusedWhenWaitingWouldDeadlockEveryUnitOfWork() throws Exception {
        // cap 2, both slots held by two distinct units, each about to ask for a second
        final Throttle throttle = throttle(2);
        final Permit u1 = throttle.acquire("acme", Duration.ZERO);
        final Permit u2 = throttle.acquire("acme", Duration.ZERO);
        assertThat(throttle.stats().units()).isEqualTo(2);

        // u1 parks as a child waiter: safe, because u2 is still working and will release.
        // Daemon: nothing ever releases in this test, so the thread outlives it and must not hold up JVM exit.
        final Thread parked = new Thread(() -> {
            try {
                u1.acquire(WAIT);
            } catch (final ThrottledException e) {
                // never granted here; the assertion below is what matters
            }
        });
        parked.setDaemon(true);
        parked.start();
        settle();

        // u2 is the last unit not already waiting: parking it would leave nobody able to release
        assertThatExceptionOfType(ThrottledException.class).isThrownBy(() -> u2.acquire(WAIT)).withMessageContaining("deadlock");
    }

    @Test
    void childStillParksWhileAnotherUnitCanRelease() throws Exception {
        final Throttle throttle = throttle(2);
        final Permit u1 = throttle.acquire("acme", Duration.ZERO);
        final Permit u2 = throttle.acquire("acme", Duration.ZERO);

        final AtomicReference<Permit> child = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        new Thread(() -> {
            child.set(u1.acquire(WAIT));
            done.countDown();
        }).start();
        settle();
        assertThat(child.get()).isNull(); // parked, not refused — u2 is still working

        u2.close();
        assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(child.get()).isNotNull();
    }

    /**
     * Builds the engine through the same key → {@link Policy} wiring the operator-facing configuration produces,
     * so the per-tenant limit lookup is covered too rather than a hand-rolled policy. Threshold 0 means always
     * contended, i.e. ceilings are enforced from the first permit — the interesting mode for every test here.
     */
    private static Throttle throttle(final int capacity) {
        return throttle(capacity, config -> { });
    }

    private static Throttle throttle(final int capacity, final Consumer<ThrottleConfig> customizer) {
        final ThrottleConfig config = new ThrottleConfig();
        config.setThreshold(0);
        config.setSystemFloor(0); // priority off unless a test opts in; the property default is 1
        customizer.accept(config);
        return new Throttle(config.toConfig(capacity));
    }

    private static void settle() throws InterruptedException {
        Thread.sleep(SETTLE.toMillis());
    }

    /**
     * The operator-facing knobs, mirroring what the autoconfigure properties bind — kept here because hawkbit-core
     * cannot depend on that module, and because a hand-built {@link Config} would skip the key → policy resolution
     * that is half of what these tests are about.
     */
    private static final class ThrottleConfig {

        // tenant -> ceiling on concurrently held permits; -1 (and any tenant not listed) falls back to `limit`
        private final Map<String, Integer> tenants = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private int threshold = -1;
        private int limit = -1; // global per-tenant ceiling, -1 = unlimited (bounded only by capacity)
        private int systemFloor;
        // the engine gates waiting on the configured timeout as well as the per-call one; keep it out of the way
        // so that, as in these tests, the value passed to acquire() is what decides
        private Duration timeout = Duration.ofMinutes(1);

        private Map<String, Integer> getTenants() {
            return tenants;
        }

        private void setThreshold(final int threshold) {
            this.threshold = threshold;
        }

        private void setLimit(final int limit) {
            this.limit = limit;
        }

        private void setSystemFloor(final int systemFloor) {
            this.systemFloor = systemFloor;
        }

        private void setTimeout(final Duration timeout) {
            this.timeout = timeout;
        }

        private Config toConfig(final int capacity) {
            // null key <=> internal, non-tenant work - the only key granted the priority floor
            final Policy systemPolicy = Policy.builder().permits(capacity).timeout(timeout).priority(Priority.HIGH).build();
            return Config.builder()
                    .permits(capacity)
                    .timeout(timeout)
                    .keyPolicyThreshold(Math.min(threshold, capacity))
                    .priorityToGranted(new EnumMap<>(Map.of(Priority.HIGH, Math.clamp(systemFloor, 0, capacity))))
                    .keyPolicyFn(key -> key == null
                            ? systemPolicy
                            : Policy.builder().permits(tenants.getOrDefault(key, limit)).timeout(timeout).build())
                    .build();
        }
    }
}
