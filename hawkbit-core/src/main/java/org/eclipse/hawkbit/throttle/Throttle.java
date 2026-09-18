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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;

/**
 * Contention-aware, priority- and fair-share admission engine over a single finite resource (e.g. the DB connection pool, REST APIs).
 *
 * <p>Every borrow of the guarded resource takes a permit, with no exempt path. That makes granted permits an exact mirror
 * of resource usage, which in turn lets this engine be the <em>only</em> queue in front of the resource: if {@link Policy#capacity()} equals
 * the real resource size, the resource itself never has to block.
 *
 * <p>A permit is charged to a <b>key</b> — e.g. a tenant id, or {@code null} for internal, non-tenant work (schedulers, health checks).
 *
 * <p>Permits form a tree per unit of work: {@link #acquire(String, Duration)} starts one, {@link Permit#acquire(Duration)}
 * joins it. A child request comes from a caller that already holds part of the resource, which is the only way this
 * engine can deadlock — see {@link Permit} for the full parent/child contract.
 *
 * <p><b>Admission order</b> (first match wins):
 * <ol>
 *   <li><b>Children</b> — bypass fair share entirely. A caller blocked while holding part of the resource is what deadlocks it, so unblocking
 *       work already in flight always outranks admitting new work.</li>
 *   <li>Keys below their {@link Policy#priorityFloor(String)} — a floor internal work cannot be starved below, costing no reserved capacity,
 *       since tenants use those slots freely whenever internal work is not.</li>
 *   <li>Below {@link Policy#burstThreshold()} — anyone may burst.</li>
 *   <li>At or above the threshold — dynamic fair share, {@code min(ceiling, capacity / activeKeys)}.</li>
 * </ol>
 *
 * <p><b>Fairness across keys is round-robin, not first-come-first-served by key.</b> Waiters queue per key, and the keys
 * themselves are ordered; whenever a key is served it rotates to the back. So with tenants A and B waiting, service goes
 * A, B, A, B — rather than draining A's whole backlog because A arrived first. Within a key, waiters are strict FIFO.
 * Only <em>service</em> rotates a key: a waiter that times out, and a key skipped for being at its fair share, both
 * leave the order untouched, so neither is penalised.
 *
 * <p><b>Deadlock</b> is avoided rather than prevented, so no capacity is reserved. A child request may block only while
 * at least one other unit of work is still running and will therefore release. The arrival that would leave every unit
 * waiting on another is refused instead of parked.
 *
 * <p>Waiting is wake-exactly-one: each waiter parks on its own {@link Condition} and a freed slot is granted to a single
 * chosen waiter, avoiding thundering herd and starvation.
 */
@Slf4j
public class Throttle {

    private final Policy policy;

    private final ReentrantLock lock = new ReentrantLock();
    private int globalInUse;
    private final Map<String, Integer> perKeyInUse = new HashMap<>();

    // permits held per unit of work, keyed on the tree's root permit; entry removed at zero. Self-bounding: every unit
    // owns at least one permit, so this can never exceed capacity entries — it cannot leak the way a caller-side
    // thread registry can, because an entry only exists while a permit does.
    private final Map<PermitImpl, Integer> permitsByRoot = new HashMap<>();

    // wait state: per-key FIFO of waiters + the keys themselves in service order (rotated on every admission).
    private final Map<String, Deque<PermitImpl>> waitersByKey = new HashMap<>();
    // LinkedList, not ArrayDeque: the system key is null and ArrayDeque rejects null elements.
    private final Deque<String> waitingKeys = new LinkedList<>();

    // child waiters across all keys, in arrival order; drives both admission tier 1 and the cycle check
    private final Deque<PermitImpl> childWaiters = new ArrayDeque<>();

    // contention tracking for WARN-on-transition logging
    private boolean wasAboveThreshold;
    private boolean wasAtCapacity;

    public Throttle(final Policy policy) {
        this.policy = policy;
    }

    /**
     * Acquire a permit for a new unit of work, blocking up to {@code timeout} while not admissible.
     *
     * @param key the key to charge the permit to; {@code null} for internal, non-tenant work
     * @param timeout maximum time to wait; {@link Duration#ZERO} (or negative) fast-rejects instead of blocking
     * @return a root {@link Permit}; call {@link Permit#close()} to release the slot, or {@link Permit#acquire(Duration)} to take more of
     *         the resource for the same unit of work
     * @throws ThrottledException if no slot became available within the timeout
     */
    public Permit acquire(final String key, final Duration timeout) {
        return acquire(key, null, timeout);
    }

    /**
     * Permits currently held, in total and per key, plus the number of distinct units of work holding them.
     * Read-only introspection for metrics and health reporting — and the only way to observe the invariant this
     * design rests on, that permits are a one-to-one mirror of the guarded resource.
     */
    public Stats stats() {
        lock.lock();
        try {
            // HashMap copy, not Map.copyOf: the system key is null, which Map.copyOf rejects
            return new Stats(globalInUse, Collections.unmodifiableMap(new HashMap<>(perKeyInUse)), permitsByRoot.size());
        } finally {
            lock.unlock();
        }
    }

    // single entry point; parent == null starts a new unit of work, otherwise the permit joins the parent's
    // java:S2093 - permit must be closed by caller site
    // java:S899 - awaitNanos result intentionally ignored; the deadline is re-checked by the loop
    @SuppressWarnings({ "java:S2093", "java:S899", "java:S3776" })
    private Permit acquire(final String key, final PermitImpl parent, final Duration timeout) {
        lock.lock();
        try {
            if (parent == null && ceiling(key) == 0) {
                throw new ThrottledException("'" + key + "' is throttled (blocked)");
            }
            if (parent != null && !permitsByRoot.containsKey(parent.root)) {
                throw new IllegalStateException("cannot acquire from a stale permit (key '" + key + "')");
            }

            final PermitImpl permit = new PermitImpl(key, parent, lock.newCondition());
            permit.enqueue();
            try {
                grantNextEligible(); // may grant this permit immediately if a slot is free

                if (!permit.admitted) {
                    // Refuse rather than park when parking would leave every unit of work waiting on another.
                    if (permit.isChild() && childWaiters.size() >= permitsByRoot.size()) { // cycle break
                        log.debug("{} is throttled: nested request refused, waiting would deadlock all {} units of work holding {}/{} slots",
                                key, permitsByRoot.size(), globalInUse, policy.capacity());
                        throw new ThrottledException("'" + key + "' is throttled (nested / deadlock)");
                    }

                    if (timeout.isPositive()) {
                        final long deadline = System.nanoTime() + timeout.toNanos();
                        long remaining;
                        while (!permit.admitted && (remaining = deadline - System.nanoTime()) > 0) {
                            try {
                                permit.condition.awaitNanos(remaining);
                            } catch (final InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break; // granted just before the interrupt is honored by the caller's admitted check
                            }
                        }
                    } // else → fast reject; caller throws
                }

                if (!permit.admitted) {
                    throw new ThrottledException("'" + key + "' is throttled (" + (timeout.isPositive() ? "timeout)" : "limit, fast-reject)"));
                }
                return permit;
            } finally {
                permit.dequeue(); // idempotent: no-op if already removed by grantNextEligible
                grantNextEligible(); // dequeue have changed the active connection and some may have become eligible
            }
        } finally {
            lock.unlock();
        }
    }

    // grants at most one waiter; must be called under lock
    private void grantNextEligible() {
        final int global = globalInUse;
        final int capacity = policy.capacity();
        if (global >= capacity) {
            log.debug("grantNextEligible() → no admit (resource full: {}/{})", global, capacity);
            return;
        }
        if (waitingKeys.isEmpty()) {
            return;
        }

        // tier 1: children bypass fair share — unblocking a unit of work releases more than it takes
        final PermitImpl child = childWaiters.peekFirst();
        if (child != null) {
            log.debug("grantNextEligible[{}] → admit (child, {}/{})", child.key, global, capacity);
            child.admit();
            return;
        }

        // Pick first, admit after: admit() rotates the served key, which mutates waitingKeys — doing that while an
        // iterator over it is still live would be a ConcurrentModificationException waiting to happen.
        final PermitImpl selected = selectWaiter(global, capacity);
        if (selected != null) {
            selected.admit();
        }
    }

    /**
     * The waiter to serve, or {@code null} if nobody is admissible. Returns the waiter rather than its key because
     * {@code null} is itself a valid key. Scans {@link #waitingKeys} in service order, so a key rotated to the back by
     * an earlier grant is considered last. Must be called under lock.
     */
    private PermitImpl selectWaiter(final int global, final int capacity) {
        // tier 2: keys still below their priority floor
        for (final String key : waitingKeys) {
            final int floor = policy.priorityFloor(key);
            if (floor > 0 && perKeyInUse.getOrDefault(key, 0) < floor) {
                log.debug("grantNextEligible[{}] → admit (priority floor {}, {}/{})", key, floor, global, capacity);
                return waitersByKey.get(key).peekFirst();
            }
        }

        // tier 3: below the contention threshold everyone may burst.
        // threshold == -1 (or 0) means always enforce the fair share (no burst allowance)
        final int threshold = policy.burstThreshold();
        if (threshold > 0 && global < threshold) {
            final String key = waitingKeys.peekFirst();
            log.debug("grantNextEligible[{}] → admit (burst: {}/{}, threshold={})", key, global, capacity, threshold);
            return waitersByKey.get(key).peekFirst();
        }

        // tier 4: dynamic fair share
        final int active = countActiveKeys();
        final int dynamicShare = Math.ceilDiv(capacity, active);
        for (final String key : waitingKeys) {
            final int keyCeiling = ceiling(key);
            final int fairShare = Math.min(keyCeiling, dynamicShare);
            final int keyCurrent = perKeyInUse.getOrDefault(key, 0);
            final boolean admit = keyCurrent < fairShare;
            log.debug("grantNextEligible[{}] → {} (current={}, fairShare=min({}, {})={}, global={}/{}, active={}, threshold={})",
                    key, admit, keyCurrent, keyCeiling, dynamicShare, fairShare, global, capacity, active, threshold);
            if (admit) {
                return waitersByKey.get(key).peekFirst();
            }
        }
        return null;
    }

    // Count total active keys (holding + waiting). Must be called under lock
    private int countActiveKeys() {
        final Set<String> all = new HashSet<>(perKeyInUse.keySet());
        all.addAll(waitingKeys);
        return all.size();
    }

    private int ceiling(final String key) {
        final int capacity = policy.capacity();
        final int limit = policy.ceiling(key);
        return limit < 0 ? capacity : Math.min(limit, capacity);
    }

    /** Admission policy. Everything operator-tunable lives here so the engine itself stays pure. */
    public interface Policy {

        /** Total permits, i.e. the size of the guarded resource. */
        int capacity();

        /**
         * Global in-use count at which fair-share enforcement activates. Below it any key may burst freely.
         * {@code -1} disables bursting, i.e. always enforce the fair share.
         */
        int burstThreshold();

        /**
         * Per-key ceiling on concurrently held permits; {@code -1} = unlimited (bounded only by capacity).
         *
         * @param key the key, {@code null} for internal non-tenant work
         */
        int ceiling(String key);

        /**
         * Permits this key is served ahead of the queue for, while holding fewer than that many. {@code 0} = no priority permits.
         * Acts as a floor the key cannot be starved below, not as a cap — beyond it the key competes normally, which is what keeps
         * a privileged key from crowding everyone else out.
         *
         * @param key the key, {@code null} for internal non-tenant work
         */
        int priorityFloor(String key);
    }

    /**
     * A consistent snapshot of resource usage, taken under the engine's lock.
     *
     * @param inUse total permits held
     * @param perKey permits held per key; keys with none are absent. The {@code null} key is internal, non-tenant work
     * @param units distinct units of work holding at least one permit — fewer than {@code inUse} when nesting is in play
     */
    public record Stats(int inUse, Map<String, Integer> perKey, int units) {

        /** Permits held by {@code key}; {@code null} for internal, non-tenant work. */
        public int inUse(final String key) {
            return perKey.getOrDefault(key, 0);
        }
    }

    private final class PermitImpl implements Permit {

        private final String key;
        private final PermitImpl root; // this, for a root permit
        private final Condition condition;
        private boolean admitted;
        private boolean closed;

        private PermitImpl(final String key, final PermitImpl parent, final Condition condition) {
            this.key = key;
            // flatten arbitrary nesting depth onto the tree root, so one unit of work is one entry in permitsByRoot
            this.root = parent == null ? this : parent.root;
            this.condition = condition;
        }

        @Override
        public Permit acquire(final Duration timeout) {
            return Throttle.this.acquire(key, this, timeout);
        }

        @Override
        public boolean isRootStale() {
            lock.lock();
            try {
                return !permitsByRoot.containsKey(root);
            } finally {
                lock.unlock();
            }
        }

        private boolean isChild() {
            return root != this;
        }

        @Override
        public void close() {
            lock.lock();
            try {
                if (!admitted || closed) {
                    return; // idempotent, and a permit that was never granted owns nothing to release
                }
                closed = true;
                globalInUse--;
                perKeyInUse.compute(key, (k, count) -> (count == null || count <= 1) ? null : count - 1);
                permitsByRoot.compute(root, (r, count) -> (count == null || count <= 1) ? null : count - 1);
                logContentionStateIfChanged();
                grantNextEligible();
            } finally {
                lock.unlock();
            }
        }

        // must be called just after construction, under lock
        private void enqueue() {
            final Deque<PermitImpl> queue = waitersByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
            if (queue.isEmpty()) {
                waitingKeys.addLast(key);
            }
            queue.addLast(this);
            if (isChild()) {
                childWaiters.addLast(this);
            }
        }

        // must be called under lock
        private void dequeue() {
            if (isChild()) {
                childWaiters.remove(this);
            }
            final Deque<PermitImpl> queue = waitersByKey.get(key);
            if (queue != null && queue.remove(this) && queue.isEmpty()) {
                waitersByKey.remove(key);
                waitingKeys.remove(key);
            }
        }

        // must be called under lock
        private void admit() {
            dequeue(); // remove now so it cannot be granted twice
            // served -> behind every other waiting key
            if (waitersByKey.containsKey(key) && waitingKeys.remove(key)) {
                waitingKeys.addLast(key);
            }
            globalInUse++;
            perKeyInUse.merge(key, 1, Integer::sum);
            permitsByRoot.merge(root, 1, Integer::sum);
            logContentionStateIfChanged();
            admitted = true;
            condition.signal();
        }

        // must be called under lock
        private void logContentionStateIfChanged() {
            final int global = globalInUse;
            final int capacity = policy.capacity();
            final int threshold = policy.burstThreshold();
            final boolean nowAboveThreshold = threshold > 0 && global >= threshold;
            final boolean nowAtCapacity = global >= capacity;

            // log threshold crossing (fairness activation)
            if (nowAboveThreshold && !wasAboveThreshold) {
                log.warn(
                        "Throttle contention threshold reached: {}/{} slots in use (threshold={} slots), fairness enforcement active. Active keys: {}",
                        global, capacity, threshold, perKeyInUse.keySet());
                wasAboveThreshold = true;
            } else if (!nowAboveThreshold && wasAboveThreshold) {
                log.info("Throttle contention below threshold: {}/{} slots in use, fairness relaxed", global, capacity);
                wasAboveThreshold = false;
            }

            // log capacity exhaustion
            if (nowAtCapacity && !wasAtCapacity) {
                log.warn("Throttle capacity exhausted: {}/{} slots in use. Per-key breakdown: {}", global, capacity, perKeyInUse);
                wasAtCapacity = true;
            } else if (!nowAtCapacity && wasAtCapacity) {
                log.info("Throttle capacity freed: {}/{} slots in use", global, capacity);
                wasAtCapacity = false;
            }
        }
    }
}
