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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.ToIntFunction;

import lombok.extern.slf4j.Slf4j;

/**
 * Contention-aware, per-tenant fair-share engine over a single finite resource (e.g. the DB
 * connection pool). Pure — no Spring, no I/O.
 * <p/>
 * Admission is contention-aware: below the contention threshold any tenant may burst; above it,
 * each tenant is capped at its dynamic fair share ({@code capacity / activeTenants}, clamped to its
 * ceiling). Waiting is a fair FIFO with wake-exactly-one — each waiter parks on its own
 * {@link Condition} and a freed slot is granted to a single chosen waiter, avoiding thundering herd
 * and starvation.
 */
@Slf4j
public class TenantThrottle {

    private final int capacity;
    private final int threshold;
    private final ToIntFunction<String> tenantLimit;

    private final ReentrantLock lock = new ReentrantLock();
    private int globalInUse;
    private final Map<String, Integer> perTenant = new HashMap<>();

    // fair wait state: per-tenant FIFO of waiters + distinct waiting tenants in arrival order
    private final Map<String, Deque<Node>> waitersByTenant = new HashMap<>();
    private final Deque<String> waitingTenants = new ArrayDeque<>();

    // contention tracking for WARN-on-transition logging
    private boolean wasAboveThreshold;
    private boolean wasAtCapacity;

    public TenantThrottle(final int capacity, final int threshold, final ToIntFunction<String> tenantLimit) {
        this.capacity = capacity;
        this.threshold = threshold;
        this.tenantLimit = tenantLimit;
    }

    /**
     * Acquire a permit for the tenant, blocking up to {@code timeout} while over the fair share.
     *
     * @param tenant the tenant to charge the permit to
     * @param timeout maximum time to wait; {@link Duration#ZERO} fast-rejects instead of blocking
     * @return a {@link Permit}; call {@link Permit#close()} to release the slot
     * @throws ThrottledException if no slot became available within the timeout
     */
    public Permit acquire(final String tenant, final Duration timeout) {
        lock.lock();
        try {
            final Node node = new Node(tenant, lock.newCondition());
            enqueue(node);
            try {
                grantNextEligible(); // may grant this node immediately if a slot is free
                final boolean fastReject = timeout.isZero() || timeout.isNegative();
                final long deadline = System.nanoTime() + timeout.toNanos();
                while (!node.admitted) {
                    if (fastReject) {
                        throw throttled(tenant);
                    }
                    final long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) {
                        throw throttled(tenant);
                    }
                    try {
                        node.condition.awaitNanos(remaining);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        if (!node.admitted) {
                            throw throttled(tenant);
                        }
                        // granted just before interruption -> honor the grant, do not leak the permit
                    }
                }
                return () -> release(tenant);
            } finally {
                removeFromQueue(node.tenant, node); // idempotent: no-op if already removed by grantNextEligible
            }
        } finally {
            lock.unlock();
        }
    }

    public int inUse() {
        lock.lock();
        try {
            return globalInUse;
        } finally {
            lock.unlock();
        }
    }

    public int inUse(final String tenant) {
        lock.lock();
        try {
            return perTenant.getOrDefault(tenant, 0);
        } finally {
            lock.unlock();
        }
    }

    private void release(final String tenant) {
        lock.lock();
        try {
            globalInUse--;
            perTenant.compute(tenant, (t, count) -> (count == null || count <= 1) ? null : count - 1);
            logContentionStateIfChanged();
            grantNextEligible();
        } finally {
            lock.unlock();
        }
    }

    // grants at most one waiter, in arrival order across tenants; must be called under lock
    private void grantNextEligible() {
        for (final String tenant : waitingTenants) {
            if (canAdmit(tenant)) {
                final Node node = waitersByTenant.get(tenant).peekFirst();
                removeFromQueue(tenant, node); // remove now so it cannot be granted twice
                admit(tenant);
                node.admitted = true;
                node.condition.signal();
                return;
            }
        }
    }

    // must be called under lock
    private boolean canAdmit(final String tenant) {
        final int global = globalInUse;
        if (global >= capacity) {
            log.debug("canAdmit({}) → false (pool full: {}/{})", tenant, global, capacity);
            return false; // pool full
        }
        // threshold=-1 means always enforce limit (no burst allowance)
        // threshold>=0 means burst freely below threshold, enforce limit above
        if (threshold >= 0 && global < threshold) {
            log.debug("canAdmit({}) → true (burst: {}/{}, threshold={})", tenant, global, capacity, threshold);
            return true; // below threshold, admit freely
        }
        // at/above threshold or threshold=-1: enforce dynamic fair share
        final int active = countActiveTenants(tenant);
        final int tenantCeiling = ceiling(tenant);
        final int dynamicShare = Math.ceilDiv(capacity, active);
        final int fairShare = Math.min(tenantCeiling, dynamicShare);
        final int tenantCurrent = perTenant.getOrDefault(tenant, 0);
        final boolean admit = tenantCurrent < fairShare;
        log.debug("canAdmit({}) → {} (current={}, fairShare=min({}, {})={}, global={}/{}, active={}, threshold={})",
                tenant, admit, tenantCurrent, tenantCeiling, dynamicShare, fairShare, global, capacity, active, threshold);
        return admit;
    }

    /**
     * Count total active tenants (running + waiting + new arrival if not yet tracked).
     * Must be called under lock.
     */
    private int countActiveTenants(final String tenant) {
        final Set<String> all = new HashSet<>(perTenant.keySet());
        all.addAll(waitingTenants);
        all.add(tenant); // new arrival; no-op if already tracked
        return all.size();
    }

    // must be called under lock
    private void admit(final String tenant) {
        globalInUse++;
        perTenant.merge(tenant, 1, Integer::sum);
        logContentionStateIfChanged();
    }

    // must be called under lock
    private void logContentionStateIfChanged() {
        final int global = globalInUse;
        final boolean nowAboveThreshold = global >= threshold;
        final boolean nowAtCapacity = global >= capacity;

        // log threshold crossing (fairness activation)
        if (nowAboveThreshold && !wasAboveThreshold) {
            log.warn(
                    "Throttle contention threshold reached: {}/{} slots in use (threshold={} slots), fairness enforcement active. Active tenants: {}",
                    global, capacity, threshold, perTenant.keySet());
            wasAboveThreshold = true;
        } else if (!nowAboveThreshold && wasAboveThreshold) {
            log.info("Throttle contention below threshold: {}/{} slots in use, fairness relaxed", global, capacity);
            wasAboveThreshold = false;
        }

        // log capacity exhaustion
        if (nowAtCapacity && !wasAtCapacity) {
            log.warn("Throttle capacity exhausted: {}/{} slots in use. Per-tenant breakdown: {}",
                    global, capacity, perTenant);
            wasAtCapacity = true;
        } else if (!nowAtCapacity && wasAtCapacity) {
            log.info("Throttle capacity freed: {}/{} slots in use", global, capacity);
            wasAtCapacity = false;
        }
    }

    private void enqueue(final Node node) {
        final Deque<Node> queue = waitersByTenant.computeIfAbsent(node.tenant, t -> new ArrayDeque<>());
        if (queue.isEmpty()) {
            waitingTenants.addLast(node.tenant);
        }
        queue.addLast(node);
    }

    private void removeFromQueue(final String tenant, final Node node) {
        final Deque<Node> queue = waitersByTenant.get(tenant);
        if (queue != null && queue.remove(node) && queue.isEmpty()) {
            waitersByTenant.remove(tenant);
            waitingTenants.remove(tenant);
        }
    }

    private int ceiling(final String tenant) {
        final int limit = tenantLimit.applyAsInt(tenant);
        return limit < 0 ? capacity : Math.min(limit, capacity);
    }

    private ThrottledException throttled(final String tenant) {
        return new ThrottledException("tenant '" + tenant + "' is throttled");
    }

    private static final class Node {

        private final String tenant;
        private final Condition condition;
        private boolean admitted;

        private Node(final String tenant, final Condition condition) {
            this.tenant = tenant;
            this.condition = condition;
        }
    }
}
