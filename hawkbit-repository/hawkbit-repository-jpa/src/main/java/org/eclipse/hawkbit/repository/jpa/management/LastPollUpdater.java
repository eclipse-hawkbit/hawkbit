/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.jpa.Jpa;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Persists the last poll time ({@link Target#getLastTargetQuery()}) of the targets whose poll changes nothing else.
 * <p/>
 * The polls are buffered, one entry per target, and written in batches by a flush - periodically
 * ({@code pollPersistenceFlushTime}) and when the buffer reaches {@code pollPersistenceFlushThresholdPercent}. If the buffer is
 * full ({@code pollPersistenceQueueSize}, 0 - always), the poll time is written directly in the poll's transaction.
 * <p/>
 * The poll time is always written by native SQL - it is not mission-critical and written only by the target itself, so the
 * optimistic lock revision isn't incremented (no {@code OptimisticLockException} in concurrent target updates) - and targets
 * locked by other transactions (e.g. a rollout group start, a device update) are skipped, never waited for.
 */
@Slf4j
class LastPollUpdater {

    private final RepositoryProperties repositoryProperties;
    private final EntityManager entityManager;
    private final PlatformTransactionManager txManager;
    private final ScheduledExecutorService executorService;

    // Buffered (not yet persisted) polls, one entry per target - a target polling again before the flush only updates its entry,
    // so the buffer is bounded by the number of distinct polling targets, not by the poll rate
    private final Map<PollKey, PollTimes> queue = new ConcurrentHashMap<>();
    // number of entries in queue - kept separately, for an exact bound (ConcurrentHashMap#size is only an estimate under updates)
    private final AtomicInteger queueSize = new AtomicInteger();
    private final int queueMaxSize;
    // number of entries added by polls since the last flush, when reached, a flush is triggered (besides the scheduled one)
    private final int flushThreshold;
    // entries added by polls since the last flush - entries queued again by a flush (e.g. locked targets) are not counted, so
    // they can't keep triggering flushes on every poll while the targets are still locked
    private final AtomicInteger addedSinceFlush = new AtomicInteger();
    // a triggered flush is submitted and not started yet - at most one is pending
    private final AtomicBoolean flushTriggered = new AtomicBoolean();
    // the scheduled and the triggered flushes run in executor threads - they must not overlap (lastFlush, lock order)
    private final ReentrantLock flushLock = new ReentrantLock();
    // start time of the previous flush - polls queued since then are written with the flush time
    private volatile long lastFlush;

    LastPollUpdater(
            final RepositoryProperties repositoryProperties,
            final EntityManager entityManager, final PlatformTransactionManager txManager, final ScheduledExecutorService executorService) {
        this.repositoryProperties = repositoryProperties;
        this.entityManager = entityManager;
        this.txManager = txManager;
        this.executorService = executorService;

        queueMaxSize = Math.max(0, repositoryProperties.getPollPersistenceQueueSize());
        flushThreshold = Math.max(
                1,
                (int) ((long) queueMaxSize * Math.clamp(repositoryProperties.getPollPersistenceFlushThresholdPercent(), 1, 100) / 100));
        if (queueMaxSize > 0) { // with no buffer every poll is written directly - nothing to flush
            executorService.scheduleWithFixedDelay(this::flush,
                    repositoryProperties.getPollPersistenceFlushTime(),
                    repositoryProperties.getPollPersistenceFlushTime(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Registers a poll of the target: buffered, or written directly in the current transaction if the buffer is full. The target
     * (entity) isn't changed - it keeps its old {@link Target#getLastTargetQuery()}.
     */
    void poll(final Target target) {
        if (!bufferPoll(target)) {
            storePollDirectly(target);
        }
    }

    // The scheduled and the triggered flushes don't wait for a running flush - it does the work, and they don't block a thread of
    // the shared executor. A flush triggered while a flush is running (the threshold reached again meanwhile) is submitted again after it.
    private void flush() {
        if (!flushLock.tryLock()) {
            return;
        }
        try {
            flush0();
        } finally {
            flushLock.unlock();
        }
        // checked after unlock: a triggered flush that didn't get the lock has set it before - it's not lost
        if (flushTriggered.get()) {
            submitFlush();
        }
    }

    private void submitFlush() {
        try {
            // as system - not in the context of the device that just polled, the flush writes the polls of all tenants
            executorService.execute(() -> AccessContext.asSystem(this::flush));
        } catch (final RuntimeException e) {
            flushTriggered.set(false); // e.g. rejected on shutdown - the scheduled flush still runs
            log.warn("Failed to trigger poll persistence flush: {}", e.getMessage());
        }
    }

    private void flush0() {
        // reset before draining - polls buffered from now on may trigger the next flush
        flushTriggered.set(false);
        addedSinceFlush.set(0);
        log.debug("Run poll persistence flush.");

        if (queueSize.get() == 0) {
            return;
        }

        final long now = System.currentTimeMillis();
        final long previousFlush = lastFlush;
        lastFlush = now;
        // per tenant: controller id -> its oldest and newest still not persisted poll. Drained entry by entry with an atomic
        // remove, so a poll merged into an entry concurrently is either drained with it or stays buffered as a new entry.
        final Map<String, Map<String, PollTimes>> pollTimesByTenant = new HashMap<>();
        int drained = 0;
        for (final PollKey key : queue.keySet()) {
            final PollTimes pollTimes = queue.remove(key);
            if (pollTimes != null) {
                queueSize.decrementAndGet();
                drained++;
                pollTimesByTenant.computeIfAbsent(key.tenant(), _ -> new HashMap<>()).put(key.controllerId(), pollTimes);
            }
        }
        log.debug("{} target polls drained.", drained);

        // per tenant, in chunks of sorted controller ids, each chunk in its own transaction: locks are held only for a chunk and
        // are taken in the same order by every flush, and a failing chunk doesn't drop the polls of the other chunks / tenants
        pollTimesByTenant.forEach((tenant, pollTimes) -> ListUtils.partition(
                        pollTimes.keySet().stream().sorted().toList(), Constants.MAX_ENTRIES_IN_STATEMENT)
                .forEach(chunk -> flushChunk(tenant, now, previousFlush, chunk, pollTimes)));

        log.debug("{} target polls processed.", drained);
    }

    // Buffers a poll of a target: merged into the target's entry if already buffered, otherwise added if the queue isn't full.
    private Buffered buffer(final String tenant, final String controllerId, final PollTimes pollTimes) {
        final Buffered[] result = new Buffered[1];
        queue.compute(new PollKey(tenant, controllerId), (_, buffered) -> {
            if (buffered != null) {
                result[0] = Buffered.MERGED;
                return buffered.merge(pollTimes);
            }
            if (queueSize.incrementAndGet() > queueMaxSize) {
                queueSize.decrementAndGet();
                result[0] = Buffered.FULL;
                return null; // not added
            }
            result[0] = Buffered.ADDED;
            return pollTimes;
        });
        return result[0];
    }

    // Buffers a poll of a device and triggers a flush when the flush threshold of newly buffered entries is reached.
    // Returns false if the queue is full.
    private boolean bufferPoll(final Target target) {
        final Buffered buffered = buffer(target.getTenant(), target.getControllerId(), new PollTimes(System.currentTimeMillis()));
        if (buffered == Buffered.FULL) {
            return false;
        }
        if (buffered == Buffered.ADDED
                && addedSinceFlush.incrementAndGet() >= flushThreshold
                && flushTriggered.compareAndSet(false, true)) {
            log.debug("Poll persistence flush threshold ({}) reached, trigger flush.", flushThreshold);
            submitFlush();
        }
        return true;
    }

    // The buffer queue is full - writes the poll time directly, in the poll's transaction, the same way as the flush does. A
    // skipped (locked) target's poll is dropped - not critical, the device polls again.
    private void storePollDirectly(final Target target) {
        final String tenant = target.getTenant();
        final String controllerId = target.getControllerId();
        final long pollTime = System.currentTimeMillis();
        if (setLastTargetQuery(tenant, List.of(controllerId), Map.of(controllerId, pollTime)).isEmpty()) {
            log.debug("Poll persistence queue is full and target {} of tenant {} is locked, dropped its poll.", controllerId, tenant);
        } else {
            afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher()
                    .publishEvent(new TargetPollEvent(List.of(controllerId), pollTime, tenant)));
        }
    }

    private void flushChunk(
            final String tenant, final long now, final long previousFlush, final List<String> chunk, final Map<String, PollTimes> pollTimes) {
        log.debug("Persist {} target queries.", chunk.size());
        // The poll time written: the flush time if the target polled since the previous flush (as the polls are buffered up to one
        // flush interval anyway, and one time for all lets them be written by one statement / event). Otherwise the target's poll
        // was queued again after a failed flush and the device didn't poll since then - its own newest poll time is written, so
        // that it doesn't look like polled later than it did.
        final Map<String, Long> pollTimeToWrite = HashMap.newHashMap(chunk.size());
        chunk.forEach(controllerId -> {
            final long newest = pollTimes.get(controllerId).newest();
            // strictly after: a poll of the previous flush's millisecond may have been drained by it
            pollTimeToWrite.put(controllerId, newest > previousFlush ? now : newest);
        });
        try {
            final List<String> skipped = DeploymentHelper.runInNewTransaction("flushUpdateQueue", _ -> {
                final List<String> written = setLastTargetQuery(tenant, chunk, pollTimeToWrite);
                if (!written.isEmpty()) {
                    // one event per written poll time - mostly one, the flush time
                    final Map<Long, List<String>> writtenByPollTime = written.stream()
                            .collect(Collectors.groupingBy(pollTimeToWrite::get, Collectors.toList()));
                    afterCommit(() -> writtenByPollTime.forEach((pollTime, controllerIds) -> EventPublisherHolder.getInstance()
                            .getEventPublisher().publishEvent(new TargetPollEvent(controllerIds, pollTime, tenant))));
                }
                return lockedByOthers(tenant, chunk, written);
            }, txManager);
            requeue(tenant, skipped, pollTimes, now);
        } catch (final RuntimeException ex) {
            log.warn("Failed to persist {} target polls of tenant {}, kept buffered for the next flush: {}", chunk.size(), tenant,
                    ex.getMessage());
            log.debug("Failed to persist target polls", ex);
            requeue(tenant, chunk, pollTimes, now);
        }
    }

    /**
     * Sets {@link Target#getLastTargetQuery()} by native SQL in order to avoid raising opt lock revision as this update is not mission-critical
     * and in fact only written by the controller management, i.e. the target itself.
     * <p/>
     * The targets are locked first, in id order, skipping the ones locked by other transactions (e.g. a rollout group start or a
     * device update) - so the flush never waits for them. Only the locked targets are updated, and a newer poll time is kept.
     *
     * @param pollTimeToWrite the poll time to write per controller id
     * @return the controller ids of the written targets
     */
    private List<String> setLastTargetQuery(final String tenant, final List<String> chunk, final Map<String, Long> pollTimeToWrite) {
        final Query lockQuery = entityManager.createNativeQuery(
                "SELECT id, controller_id FROM sp_target WHERE tenant = " + Jpa.nativeQueryParamPrefix() + "tenant" +
                        " AND controller_id IN (" + Jpa.formatNativeQueryInClause("cid", chunk) + ")" +
                        " ORDER BY id FOR UPDATE SKIP LOCKED"); // FOR UPDATE locks
        lockQuery.setParameter("tenant", tenant);
        Jpa.setNativeQueryInParameter(lockQuery, "cid", chunk);
        @SuppressWarnings("unchecked") final List<Object[]> locked = lockQuery.getResultList();
        if (locked.isEmpty()) {
            return List.of();
        }

        final List<Long> ids = locked.stream().map(row -> ((Number) row[0]).longValue()).toList();
        final List<Long> times = locked.stream().map(row -> pollTimeToWrite.get((String) row[1])).toList();
        final String prefix = String.valueOf(Jpa.nativeQueryParamPrefix());
        final boolean samePollTime = times.stream().distinct().count() == 1;
        final String pollTime;
        if (samePollTime) {
            pollTime = prefix + "pt";
        } else {
            // different poll times (rare - polls queued again after a failed flush): one statement with a time per id
            final StringBuilder caseExpression = new StringBuilder("CASE id");
            for (int i = 0; i < ids.size(); i++) {
                caseExpression.append(" WHEN ").append(prefix).append("cid").append(i).append(" THEN ").append(prefix).append("pt").append(i);
            }
            pollTime = caseExpression.append(" END").toString();
        }
        final Query updateQuery = entityManager.createNativeQuery(
                // never moves the poll time backwards, e.g. a buffered poll after a newer write of a changed target
                "UPDATE sp_target SET last_target_query = GREATEST(COALESCE(last_target_query, 0), " + pollTime + ") " +
                        "WHERE id IN (" + Jpa.formatNativeQueryInClause("id", ids) + ")");
        if (samePollTime) {
            updateQuery.setParameter("pt", times.get(0));
        } else {
            for (int i = 0; i < ids.size(); i++) {
                updateQuery.setParameter("cid" + i, ids.get(i));
                updateQuery.setParameter("pt" + i, times.get(i));
            }
        }
        Jpa.setNativeQueryInParameter(updateQuery, "id", ids);
        final int updated = updateQuery.executeUpdate();
        if (updated < ids.size()) {
            log.warn("Targets polls could not be applied completely ({} instead of {}).", updated, ids.size());
        }
        return locked.stream().map(row -> (String) row[1]).toList();
    }

    // The targets of the chunk that were not written because another transaction locked them - their polls are kept buffered.
    // The other not written ones don't exist (anymore), e.g. deleted - their polls are dropped. Plain read, doesn't wait for locks.
    private List<String> lockedByOthers(final String tenant, final List<String> chunk, final List<String> written) {
        if (written.size() == chunk.size()) {
            return List.of();
        }
        final Set<String> writtenSet = new HashSet<>(written);
        final List<String> notWritten = chunk.stream().filter(controllerId -> !writtenSet.contains(controllerId)).toList();
        final Query existingQuery = entityManager.createNativeQuery(
                "SELECT controller_id FROM sp_target WHERE tenant = " + Jpa.nativeQueryParamPrefix() + "tenant" +
                        " AND controller_id IN (" + Jpa.formatNativeQueryInClause("cid", notWritten) + ")");
        existingQuery.setParameter("tenant", tenant);
        Jpa.setNativeQueryInParameter(existingQuery, "cid", notWritten);
        @SuppressWarnings("unchecked") final List<String> skipped = existingQuery.getResultList();
        if (skipped.size() < notWritten.size()) {
            log.debug("Polls of {} not existing targets dropped.", notWritten.size() - skipped.size());
        }
        return skipped;
    }

    // Queues the polls again with their original queue time. Dropped (not critical, the devices poll again) are polls queued
    // longer than pollPersistenceMaxQueueTime - so a persistent failure doesn't keep them forever - and, non-blocking, the
    // polls that don't fit into a full queue.
    private void requeue(final String tenant, final List<String> controllerIds, final Map<String, PollTimes> pollTimes, final long now) {
        final long minQueuedAt = now - repositoryProperties.getPollPersistenceMaxQueueTime();
        int expired = 0;
        int requeued = 0;
        for (final String controllerId : controllerIds) {
            final PollTimes controllerPollTimes = pollTimes.get(controllerId);
            if (controllerPollTimes.oldest() < minQueuedAt) {
                expired++;
            } else if (buffer(tenant, controllerId, controllerPollTimes) != Buffered.FULL) {
                // merged with a poll of the target buffered in the meantime, if any - keeps its original oldest poll time
                requeued++;
            } else {
                log.debug("Poll persistence queue is full, dropped buffered polls of tenant {}: {}.",
                        tenant, controllerIds.size() - requeued - expired);
                break;
            }
        }
        if (expired > 0) {
            log.warn("Dropped {} polls of tenant {} that couldn't be persisted within {} ms.",
                    expired, tenant, repositoryProperties.getPollPersistenceMaxQueueTime());
        }
    }

    private record PollKey(String tenant, String controllerId) {}

    private enum Buffered {
        ADDED, // new entry for the target
        MERGED, // merged into the already buffered entry of the target
        FULL // not buffered - queue full
    }

    // the oldest not persisted poll of a target - limits how long it is queued again (pollPersistenceMaxQueueTime), a newer poll
    // doesn't reset it - and the newest one, the target's real last poll time
    private record PollTimes(long oldest, long newest) {

        PollTimes(final long pollTime) {
            this(pollTime, pollTime);
        }

        PollTimes merge(final PollTimes other) {
            return new PollTimes(Math.min(oldest, other.oldest), Math.max(newest, other.newest));
        }
    }
}
