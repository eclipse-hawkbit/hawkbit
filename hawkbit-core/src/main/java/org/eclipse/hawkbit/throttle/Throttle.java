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

import static org.eclipse.hawkbit.throttle.Config.PendingRemoveStrategy.LIFO;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.throttle.Config.Priority;
import org.eclipse.hawkbit.throttle.ThrottledException.Reason;

/**
 * Contention-aware, priority- and fair-share admission engine over a single finite resource (e.g. the DB connection pool, REST APIs).
 *
 * <p>Every borrow of the guarded resource takes a permit, with no exempt path. That makes granted permits an exact mirror
 * of resource usage, which in turn lets this engine be the <em>only</em> queue in front of the resource:
 * if {@link org.eclipse.hawkbit.throttle.Config.Policy#permits()} equals the real resource size, the resource itself never has to block.
 *
 * <p>A permit is charged to a <b>key</b> — e.g. a tenant id, or {@code null} for internal, non-tenant work (schedulers, health checks).
 *
 * <p>Permits form a tree per unit of work: {@link #acquire(String, Duration)} starts one, {@link Permit#acquire(Duration)} joins it.
 * A child request comes from a caller that already holds part of the resource, which is the only way this engine can deadlock — see
 * {@link Permit} for the full parent/child contract.
 *
 * <p><b>Admission order</b> (first match wins):
 * <ol>
 *   <li><b>Children</b> — bypass fair share entirely. A caller blocked while holding part of the resource is what deadlocks it, so unblocking
 *       work already in flight always outranks admitting new work.</li>
 *   <li>High priority waiters (of any key) while below the high priority ({@link Config#priorityGranted(Priority)} granted — high
 *       priority work never queues with the other work of its key, so no key backlog can block it. Costs no reserved capacity, since
 *       the others use those slots freely whenever high priority work is not.</li>
 *   <li>Below {@link Config#keyPolicyThreshold()} — anyone may burst.</li>
 *   <li>At or above the threshold — round-robin + per key max permit bound share.</li>
 *   <li>High priority waiters above the high priority granted — the idle capacity only, i.e. what no other waiter is admissible
 *       for.</li>
 * </ol>
 *
 * <p><b>Fairness across keys is round-robin, not first-come-first-served by key.</b> Waiters queue per key, and the keys
 * themselves are ordered; whenever a key is served it rotates to the back. So with tenants A and B waiting, service goes
 * A, B, A, B — rather than draining A's whole backlog because A arrived first. Within a key, waiters are strict FIFO.
 * Only <em>service</em> rotates a key: a waiter that times out, and a key skipped for being at its fair share, both
 * leave the order untouched, so neither is penalised.
 *
 * <p><b>Waiting is bounded too, not just admission.</b> A parked waiter holds its caller's thread, so an unbounded
 * queue simply moves exhaustion from the guarded resource to the thread pool in front of it. Two bounds apply, both
 * only to requests that would otherwise park: {@link Config#maxPending()} over all keys together, and
 * {@link Config#maxPending(String)} per key. On overflow one waiter is dropped rather than the arrival being
 * refused outright — for the global bound it is taken from the <em>longest</em> queue, so a key flooding the queue
 * pays for it instead of pushing a quiet key's waiters out.
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

    // priority declared for the current thread - read on acquire, i.e. in the caller thread
    private static final ThreadLocal<Priority> PRIORITY = new ThreadLocal<>();

    private final Config config;

    private final ReentrantLock lock = new ReentrantLock();

    private int globalGranted;
    private final Map<String, Integer> perKeyGranted = new HashMap<>(); // not high priority - counted to the key limit
    private int highGranted; // high priority, of any key - counted to the high priority granted, not to the key limit
    // permits held per unit of work, keyed on the tree's root permit; entry removed at zero. Self-bounding: every unit
    // owns at least one permit, so this can never exceed capacity entries — it cannot leak the way a caller-side
    // thread registry can, because an entry only exists while a permit does.
    private final Map<PermitImpl, Integer> permitsByRoot = new HashMap<>();

    private int globalPending;
    // wait state: per-key FIFO of pending + the keys themselves in service order (rotated on every admission).
    private final Map<String, Deque<PermitImpl>> perKeyPending = new HashMap<>();
    // LinkedList, not ArrayDeque: the system key is null and ArrayDeque rejects null elements.
    private final Deque<String> pendingKeys = new LinkedList<>();
    // pending children across all keys, in arrival order; drives both admission tier 1 and the cycle check
    private final Deque<PermitImpl> pendingChildren = new ArrayDeque<>();
    // pending high priority permits across all keys, in arrival order; admitted only by tiers 2 and 5
    private final Deque<PermitImpl> highPending = new ArrayDeque<>();

    // contention tracking for WARN-on-transition logging
    private boolean wasAboveThreshold;
    private boolean wasAtCapacity;

    public Throttle(final Config config) {
        this.config = config;
    }

    /**
     * Runs a given {@link Runnable} with the given priority. See {@link #withPriority(Priority, Supplier)}.
     *
     * @param priority the priority to acquire the permits with
     * @param runnable the runnable to run with the priority
     */
    public static void withPriority(final Priority priority, final Runnable runnable) {
        withPriority(priority, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Runs a given {@link Supplier} with the given priority. Root permits acquired (by the current thread) within the supplier with
     * {@link Priority#HIGH} are charged to the requested key but are not counted to its limit, and are served ahead of the other waiters
     * up to the {@link Config#priorityGranted(Priority) high priority granted}, above it on the idle capacity only - for work the server
     * itself initiated (e.g. schedulers working for a tenant), never for client requests. Other priorities have no effect. A permit
     * joining an already held one inherits the priority of its root.
     * <p>The priority is thread bound - it applies to all {@link Throttle} instances and it is not propagated to other threads.
     * Could be nested; the previous priority is restored after the call.
     *
     * @param priority the priority to acquire the permits with
     * @param supplier the supplier to call with the priority
     * @return the return value of the {@link Supplier#get()} method.
     */
    public static <T> T withPriority(final Priority priority, final Supplier<T> supplier) {
        final Priority current = PRIORITY.get();
        try {
            setPriority(priority);
            return supplier.get();
        } finally {
            setPriority(current);
        }
    }

    /**
     * @return the priority set by {@link #withPriority(Priority, Supplier)} for the current running code, empty if not set
     */
    public static Optional<Priority> priority() {
        return Optional.ofNullable(PRIORITY.get());
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
            return new Stats(globalGranted, Collections.unmodifiableMap(new HashMap<>(perKeyGranted)), highGranted, permitsByRoot.size(),
                    globalPending);
        } finally {
            lock.unlock();
        }
    }

    // single entry point; parent == null starts a new unit of work, otherwise the permit joins the parent's
    // java:S2093 - permit must be closed by caller site
    // java:S899 - awaitNanos result intentionally ignored; the deadline is re-checked by the loop
    // java:S3776 - better readable in one place
    // java:S2274 - fine, false positive of sonar requiring a `while`
    @SuppressWarnings({ "java:S2093", "java:S899", "java:S3776", "java:S2274" })
    private Permit acquire(final String key, final PermitImpl parent, final Duration timeout) {
        lock.lock();
        try {
            if (parent != null && !permitsByRoot.containsKey(parent.root)) { // handle stale parent
                throw new IllegalStateException("cannot acquire from a stale permit (key '%s')".formatted(key));
            }

            // a child inherits it from its root, so a unit of work is either high priority or not
            final Priority priority = parent == null ? priorityOf(key) : parent.root.priority;
            final PermitImpl permit = new PermitImpl(key, priority, parent, lock.newCondition());
            // handle blocked - high priority is not bound by the key
            if (parent == null && !permit.isHighPriority() && config.permits(key) == 0) {
                throw new ThrottledException(Reason.BLOCKED, "'%s' is throttled (blocked)".formatted(key));
            }
            permit.enqueue();
            try {
                grantNextEligible(); // may grant this permit immediately if a slot is free

                if (permit.state != PermitImpl.State.GRANTED) {
                    if (!timeout.isPositive()) { // no wait requestedd, fast-reject
                        throw new ThrottledException(Reason.LIMIT, "'%s' is throttled (limit)".formatted(key));
                    }

                    final Duration keyTimeout = config.timeout(key);
                    if (!keyTimeout.isPositive()) { // no wait is allowed, fast-reject
                        throw new ThrottledException(Reason.FAST_REJECT, "'%s' is throttled (limit, fast-reject)".formatted(key));
                    } // else eligible to wait

                    // handle children / deadlocks, refuse rather than park when parking would leave every unit of work waiting on another.
                    if (permit.isChild() && pendingChildren.size() >= permitsByRoot.size()) { // cycle break
                        log.debug("{} is throttled: nested request refused, waiting would deadlock all {} units of work holding {}/{} slots",
                                key, permitsByRoot.size(), globalGranted, config.permits(key));
                        throw new ThrottledException(Reason.DEADLOCK, "'%s' is throttled (nested / deadlock)".formatted(key));
                    }

                    // handle max pending, per key first - an eviction there also relieves the global bound,
                    // since the evicted permit is counted by both
                    final Deque<PermitImpl> keyPending = permit.isHighPriority() ? highPending : perKeyPending.get(key);
                    final int keyMaxPending = config.maxPending(key);
                    if (!permit.isHighPriority() && keyMaxPending >= 0 && keyPending.size() > keyMaxPending
                            && evictOrReject(keyPending, permit, config.pendingRemoveStrategy(key))) {
                        log.trace("{} is throttled: key pending queue full, this", key);
                        // throw, finally deque will remove it
                        throw new ThrottledException(Reason.KEY_QUEUE_FULL, "'%s' is throttled (key pending queue full, this)".formatted(key));
                    }

                    final int maxPending = config.maxPending();
                    if (maxPending >= 0 && globalPending > maxPending
                            && evictOrReject(longestPending(keyPending), permit, config.pendingRemoveStrategy())) {
                        log.trace("{} is throttled: pending queue full, this", key);
                        // throw, finally deque will remove it
                        throw new ThrottledException(Reason.QUEUE_FULL, "'%s' is throttled (pending queue full, this)".formatted(key));
                    }

                    final long deadline = System.nanoTime() + Math.min(timeout.toNanos(), keyTimeout.toNanos());
                    for (long remaining; permit.state == PermitImpl.State.WAITING && (remaining = deadline - System.nanoTime()) > 0; ) {
                        try {
                            permit.condition.awaitNanos(remaining);
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break; // granted just before the interrupt is honored by the caller's admitted check
                        }
                    }

                    if (permit.state != PermitImpl.State.GRANTED) {
                        // woken before the deadline means someone took this slot in the queue, not that time ran out
                        final Reason reason = deadline - System.nanoTime() > 0 ? Reason.EVICTED : Reason.TIMEOUT;
                        throw new ThrottledException(reason, "'%s' is throttled (%s)".formatted(key, reason));
                    }
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

    /**
     * Frees one slot in {@code queue} following {@code strategy}, skipping children - a child is a caller that already
     * holds part of the resource, so dropping it throws away work that is about to release rather than relieving
     * pressure. When every waiter is a child the first one scanned is taken anyway, since the bound must hold.
     *
     * @return {@code true} when the arriving permit is itself the victim and the caller must reject it
     */
    // must be called under lock
    private boolean evictOrReject(final Deque<PermitImpl> queue, final PermitImpl permit, final Config.PendingRemoveStrategy strategy) {
        final Iterator<PermitImpl> it = strategy == LIFO ? queue.descendingIterator() : queue.iterator();
        PermitImpl victim = it.next(); // may be child, must have at least one - this has been just enqueued
        while (victim.isChild() && it.hasNext()) { // try to find first non-child victim
            final PermitImpl next = it.next();
            if (!next.isChild()) {
                victim = next;
                break;
            }
        }
        if (victim == permit) {
            return true;
        }
        log.trace("{} evicted from the pending queue to make room", victim.key);
        victim.interrupt(); // dequeues (so both counters drop) and wakes it to be rejected on its own thread
        return false;
    }

    /**
     * The longest pending queue, i.e. the key responsible for the global overflow, so a key flooding the queue cannot
     * push other keys' waiters out.
     *
     * <p>Must be called under lock.
     */
    private Deque<PermitImpl> longestPending(final Deque<PermitImpl> preferred) {
        Deque<PermitImpl> longest = preferred;
        for (final Deque<PermitImpl> queue : perKeyPending.values()) {
            if (queue.size() > longest.size()) {
                longest = queue;
            }
        }
        return longest;
    }

    // grants at most one waiter; must be called under lock
    private void grantNextEligible() {
        final int global = globalGranted;
        final int capacity = config.permits();
        if (global >= capacity) {
            log.debug("grantNextEligible() → no admit (resource full: {}/{})", global, capacity);
            return;
        }
        if (pendingKeys.isEmpty() && highPending.isEmpty()) {
            return;
        }

        // tier 1: children bypass fair share — unblocking a unit of work releases more than it takes
        final PermitImpl child = pendingChildren.peekFirst();
        if (child != null) {
            log.debug("grantNextEligible[{}] → admit (child, {}/{})", child.key, global, capacity);
            child.grant();
            return;
        }

        // Pick first, admit after: admit() rotates the served key, which mutates waitingKeys — doing that while an
        // iterator over it is still live would be a ConcurrentModificationException waiting to happen.
        final PermitImpl selected = selectPending(global, capacity);
        if (selected != null) {
            selected.grant();
        }
    }

    /**
     * The waiter to serve, or {@code null} if nobody is admissible. Returns the waiter rather than its key because
     * {@code null} is itself a valid key. Scans {@link #pendingKeys} in service order, so a key rotated to the back by
     * an earlier grant is considered last. Must be called under lock.
     */
    private PermitImpl selectPending(final int global, final int capacity) {
        // tier 2: high priority waiters (of any key) while below the priority granted
        final int granted = config.priorityGranted(Priority.HIGH);
        if (highGranted < granted && !highPending.isEmpty()) {
            log.debug("grantNextEligible[high] → admit (priority granted {}, {}/{})", granted, global, capacity);
            return highPending.peekFirst();
        }

        // tier 3: below the contention threshold everyone may burst.
        // threshold <= 0 means always enforce the fair share (no burst allowance)
        final int threshold = config.keyPolicyThreshold();
        if (threshold > 0 && global < threshold && !pendingKeys.isEmpty()) {
            final String key = pendingKeys.peekFirst();
            log.debug("grantNextEligible[{}] → admit (burst: {}/{}, threshold={})", key, global, capacity, threshold);
            return perKeyPending.get(key).peekFirst();
        }

        // tier 4: up to ceiling share
        for (final String key : pendingKeys) {
            final int keyPermits = config.permits(key);
            final int keyCurrent = perKeyGranted.getOrDefault(key, 0);
            final boolean grant = keyCurrent < keyPermits;
            log.debug("grantNextEligible[{}] → {} (current={}, permits={}, global={}/{}, threshold={})",
                    key, grant, keyCurrent, keyPermits, global, capacity, threshold);
            if (grant) {
                return perKeyPending.get(key).peekFirst();
            }
        }

        // tier 5: high priority waiters above the priority granted take the idle capacity - only when no other waiter is admissible
        if (!highPending.isEmpty()) {
            log.debug("grantNextEligible[high] → admit (idle capacity, {}/{})", global, capacity);
            return highPending.peekFirst();
        }
        return null;
    }

    // read in the caller thread. The thread priority (e.g. schedulers working for a tenant), else the key policy one (e.g. the null key)
    private Priority priorityOf(final String key) {
        return PRIORITY.get() == Priority.HIGH ? Priority.HIGH : config.priority(key);
    }

    private static void setPriority(final Priority priority) {
        if (priority == null) {
            PRIORITY.remove();
        } else {
            PRIORITY.set(priority);
        }
    }

    /**
     * A consistent snapshot of resource usage, taken under the engine's lock.
     *
     * @param globalGranted total permits held
     * @param perKeyGranted permits held per key, not high priority; keys with none are absent. The {@code null} key is internal,
     *         non-tenant work
     * @param highGranted high priority permits held, of any key
     * @param units distinct units of work holding at least one permit — fewer than {@code inUse} when nesting is in play
     * @param pending waiters parked over all keys together; the value bounded by the global max pending
     */
    public record Stats(int globalGranted, Map<String, Integer> perKeyGranted, int highGranted, int units, int pending) {

        /** Permits held by {@code key}, not high priority; {@code null} for internal, non-tenant work. */
        public int perKeyGranted(final String key) {
            return perKeyGranted.getOrDefault(key, 0);
        }
    }

    private final class PermitImpl implements Permit {

        private enum State {
            WAITING, GRANTED, CLOSED /* rejected or granted and then closed*/
        }

        private final String key;
        private final Priority priority;
        private final PermitImpl root;
        private final Condition condition;
        private State state = State.WAITING;

        private PermitImpl(final String key, final Priority priority, final PermitImpl parent, final Condition condition) {
            this.key = key;
            this.priority = priority;
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

        // served apart from the key work only if high priority is granted, otherwise the priority is ignored
        private boolean isHighPriority() {
            return priority == Priority.HIGH && config.priorityGranted(Priority.HIGH) > 0;
        }

        @Override
        public void close() {
            lock.lock();
            try {
                if (state != State.GRANTED) {
                    return; // idempotent, only GRANTED -> CLOSED transition
                }

                state = State.CLOSED;
                globalGranted--;
                if (isHighPriority()) {
                    highGranted--;
                } else {
                    perKeyGranted.compute(key, (k, count) -> (count == null || count <= 1) ? null : count - 1);
                }
                permitsByRoot.compute(root, (r, count) -> (count == null || count <= 1) ? null : count - 1);
                logContentionStateIfChanged();
                grantNextEligible();
            } finally {
                lock.unlock();
            }
        }

        // must be called under lock
        private void interrupt() {
            if (state != State.WAITING) {
                throw new IllegalStateException("Should be called only in WAITING state");
            }

            state = State.CLOSED;
            // remove now (earlier, in interrupter thread) to do not wait for this permit thread to get the lock to dequeue (and count it by then)
            dequeue();
            condition.signal();
        }

        // must be called just after construction, under lock
        private void enqueue() {
            final Deque<PermitImpl> queue = isHighPriority() ? highPending : perKeyPending.computeIfAbsent(key, k -> new ArrayDeque<>());
            if (!isHighPriority() && queue.isEmpty()) {
                pendingKeys.addLast(key);
            }
            queue.addLast(this);
            globalPending++;
            if (isChild()) {
                pendingChildren.addLast(this);
            }
        }

        // must be called under lock; idempotent - the removal from the key queue is what decides whether this
        // permit was still pending, so every other bit of wait state is unwound exactly once with it
        private void dequeue() {
            final Deque<PermitImpl> queue = isHighPriority() ? highPending : perKeyPending.get(key);
            if (queue == null || !queue.remove(this)) {
                return; // already dequeued by grant() or interrupt()
            }

            globalPending--;
            if (isChild()) {
                pendingChildren.remove(this);
            }
            if (!isHighPriority() && queue.isEmpty()) {
                perKeyPending.remove(key);
                pendingKeys.remove(key);
            }
        }

        // must be called under lock
        private void grant() {
            dequeue(); // remove now so it cannot be granted twice
            // served -> behind every other waiting key
            if (!isHighPriority() && perKeyPending.containsKey(key) && pendingKeys.remove(key)) {
                pendingKeys.addLast(key);
            }
            globalGranted++;
            if (isHighPriority()) {
                highGranted++;
            } else {
                perKeyGranted.merge(key, 1, Integer::sum);
            }
            permitsByRoot.merge(root, 1, Integer::sum);
            logContentionStateIfChanged();
            state = State.GRANTED;
            condition.signal();
        }

        // must be called under lock
        private void logContentionStateIfChanged() {
            final int global = globalGranted;
            final int permits = config.permits();
            final int keyPolicyThreshold = config.keyPolicyThreshold();
            final boolean nowAboveThreshold = keyPolicyThreshold > 0 && global >= keyPolicyThreshold;
            final boolean nowAtCapacity = global >= permits;

            // log threshold crossing (fairness activation)
            if (nowAboveThreshold && !wasAboveThreshold) {
                log.warn(
                        "Throttle contention threshold reached: {}/{} slots in use (key policy threshold={} slots), fairness enforcement active. Active keys: {}",
                        global, permits, keyPolicyThreshold, perKeyGranted.keySet());
                wasAboveThreshold = true;
            } else if (!nowAboveThreshold && wasAboveThreshold) {
                log.info("Throttle contention below threshold: {}/{} slots in use, fairness relaxed", global, permits);
                wasAboveThreshold = false;
            }

            // log capacity exhaustion
            if (nowAtCapacity && !wasAtCapacity) {
                log.warn("Throttle capacity exhausted: {}/{} slots in use. Per-key breakdown: {}", global, permits, perKeyGranted);
                wasAtCapacity = true;
            } else if (!nowAtCapacity && wasAtCapacity) {
                log.info("Throttle capacity freed: {}/{} slots in use", global, permits);
                wasAtCapacity = false;
            }
        }
    }
}
