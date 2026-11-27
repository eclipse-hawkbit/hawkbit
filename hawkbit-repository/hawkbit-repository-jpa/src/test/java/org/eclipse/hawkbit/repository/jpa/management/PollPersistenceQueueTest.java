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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bounds of the buffered (lazy) target poll queue: one entry per target, a flush triggered by the fill level and the
 * direct write of the poll time when the queue is full. The scheduled flush is effectively off (flush time of one hour),
 * queue size 4 with a flush threshold of 50% - a flush is triggered by 2 newly buffered targets.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Poll persistence
 */
@TestPropertySource(locations = "classpath:/jpa-test.properties", properties = {
        "hawkbit.server.repository.pollPersistenceFlushTime=3600000",
        "hawkbit.server.repository.pollPersistenceQueueSize=4",
        "hawkbit.server.repository.pollPersistenceFlushThresholdPercent=50" })
class PollPersistenceQueueTest extends AbstractJpaIntegrationTest {

    private static final Duration AWAIT = Duration.ofSeconds(10);

    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationEventMulticaster eventMulticaster;
    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    private final List<TargetPollEvent> pollEvents = new CopyOnWriteArrayList<>();
    private final ApplicationListener<TargetPollEvent> pollEventListener = pollEvents::add;

    private LastPollUpdater lastPollUpdater;

    @BeforeEach
    void setUp() {
        eventMulticaster.addApplicationListener(pollEventListener);
        lastPollUpdater = (LastPollUpdater) ReflectionTestUtils.getField(
                (Object) AopTestUtils.getUltimateTargetObject(controllerManagement),
                "lastPollUpdater");
        // nothing left from other tests
        flush();
    }

    @AfterEach
    void tearDown() throws Exception {
        // a triggered flush still pending mustn't run into the next test: flush (resets the trigger) and drain the executor
        flush();
        awaitExecutorIdle();
        assertThat(flushTriggered().get()).isFalse();
        eventMulticaster.removeApplicationListener(pollEventListener);
    }

    /**
     * Verifies that repeated polls of a target take one queue entry and are written once, with one poll event.
     */
    @Test
    void repeatedPollsOfTargetTakeOneEntry() throws Exception {
        final Target t1 = registerAndPoll("dedup-t1");
        controllerManagement.findOrRegisterTargetIfItDoesNotExist(t1.getControllerId(), LOCALHOST);
        controllerManagement.findOrRegisterTargetIfItDoesNotExist(t1.getControllerId(), LOCALHOST);
        assertThat(queueSize().get()).isEqualTo(1);
        resetPollEvents();

        flush();
        waitForEvents();

        assertThat(queueSize().get()).isZero();
        assertThat(lastTargetQuery(t1)).isGreaterThan(t1.getLastTargetQuery());
        assertThat(polledControllerIds()).containsExactly(t1.getControllerId());
    }

    /**
     * Verifies that a flush is triggered when the flush threshold of newly buffered targets is reached, without waiting for
     * the scheduled flush.
     */
    @Test
    void flushTriggeredWhenThresholdReached() throws Exception {
        final Target t1 = registerAndPoll("threshold-t1");
        assertThat(queueSize().get()).isEqualTo(1);
        final Target t2 = registerAndPoll("threshold-t2");

        await().pollInSameThread().atMost(AWAIT).untilAsserted(() -> {
            assertThat(lastTargetQuery(t1)).isGreaterThan(t1.getLastTargetQuery());
            assertThat(lastTargetQuery(t2)).isGreaterThan(t2.getLastTargetQuery());
        });
        assertThat(queueSize().get()).isZero();
    }

    /**
     * Verifies that the poll of a target is written directly when the queue is full - immediately, without changing the
     * target's optimistic lock version, and without adding an entry.
     */
    @Test
    void pollWrittenDirectlyWhenQueueFull() throws Exception {
        final ReentrantLock flushLock = flushLock();
        final Target[] buffered = new Target[4];
        final Target direct;
        final JpaTarget registered;
        // the flush triggered by the threshold doesn't get the lock and skips, so the queue stays full
        flushLock.lock();
        try {
            for (int i = 0; i < buffered.length; i++) {
                buffered[i] = registerAndPoll("full-t" + i);
            }
            assertThat(queueSize().get()).isEqualTo(4);

            direct = controllerManagement.findOrRegisterTargetIfItDoesNotExist("full-direct", LOCALHOST);
            // the registration is an eager write - compare the version after it
            registered = targetRepository.findById(direct.getId()).orElseThrow();
            TimeUnit.MILLISECONDS.sleep(5);
            resetPollEvents();

            controllerManagement.findOrRegisterTargetIfItDoesNotExist("full-direct", LOCALHOST);
            waitForEvents();

            final JpaTarget after = targetRepository.findById(direct.getId()).orElseThrow();
            assertThat(after.getLastTargetQuery()).isGreaterThan(registered.getLastTargetQuery());
            assertThat(after.getOptLockRevision()).isEqualTo(registered.getOptLockRevision());
            assertThat(queueSize().get()).isEqualTo(4);
            assertThat(polledControllerIds()).containsExactly(direct.getControllerId());
            for (final Target target : buffered) {
                assertThat(lastTargetQuery(target)).isEqualTo(target.getLastTargetQuery());
            }
        } finally {
            flushLock.unlock();
        }

        // the lock was held directly, not by a flush - nothing resubmits the skipped flush, the buffered polls stay queued
        awaitExecutorIdle();
        assertThat(queueSize().get()).isEqualTo(4);

        flush();

        for (final Target target : buffered) {
            assertThat(lastTargetQuery(target)).isGreaterThan(target.getLastTargetQuery());
        }
        assertThat(queueSize().get()).isZero();
    }

    /**
     * Verifies that a flush triggered while another flush runs is skipped (doesn't block the executor thread) and is
     * submitted again when the running flush finishes, so the polls buffered meanwhile are written without waiting for the
     * scheduled flush.
     */
    @Test
    void flushTriggeredDuringRunningFlushIsResubmitted() throws Exception {
        final Target t0 = registerAndPoll("resubmit-t0");
        final CountDownLatch flushBlocked = new CountDownLatch(1);
        final CountDownLatch releaseFlush = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final EntityManager original = (EntityManager) ReflectionTestUtils.getField(lastPollUpdater, "entityManager");
        final Thread runningFlush = new Thread(new DelegatingSecurityContextRunnable(() -> {
            try {
                flush();
            } catch (final Throwable e) {
                failure.set(e);
            }
        }), "running-flush");
        // the first native query of the running flush blocks until released - the flush holds the flush lock meanwhile
        ReflectionTestUtils.setField(lastPollUpdater, "entityManager",
                blockingFirstNativeQuery(original, runningFlush, flushBlocked, releaseFlush));
        final Target t1;
        final Target t2;
        try {
            runningFlush.start();
            assertThat(flushBlocked.await(10, TimeUnit.SECONDS)).isTrue();

            t1 = registerAndPoll("resubmit-t1");
            t2 = registerAndPoll("resubmit-t2"); // threshold reached - triggered flush skips, the lock is held
            awaitExecutorIdle();
            assertThat(flushTriggered().get()).isTrue();
            assertThat(queueSize().get()).isEqualTo(2);
            assertThat(lastTargetQuery(t1)).isEqualTo(t1.getLastTargetQuery());
        } finally {
            releaseFlush.countDown();
            runningFlush.join(TimeUnit.SECONDS.toMillis(10));
            ReflectionTestUtils.setField(lastPollUpdater, "entityManager", original);
        }
        assertThat(failure.get()).isNull();
        assertThat(lastTargetQuery(t0)).isGreaterThan(t0.getLastTargetQuery());

        // resubmitted by the running flush after it released the lock
        await().pollInSameThread().atMost(AWAIT).untilAsserted(() -> {
            assertThat(lastTargetQuery(t1)).isGreaterThan(t1.getLastTargetQuery());
            assertThat(lastTargetQuery(t2)).isGreaterThan(t2.getLastTargetQuery());
        });
        assertThat(queueSize().get()).isZero();
    }

    /**
     * Verifies that polls queued again by a flush (locked target) aren't counted for the flush threshold - they don't
     * trigger a flush together with a newly buffered target.
     */
    @Test
    void requeuedPollsDontCountForThreshold() throws Exception {
        final Target t1 = registerAndPoll("requeued-t1");
        try (final TargetLock _ = lock(t1)) {
            flush();
        }
        assertThat(queueSize().get()).isEqualTo(1);
        assertThat(addedSinceFlush().get()).isZero();

        final Target t2 = registerAndPoll("requeued-t2");

        assertThat(queueSize().get()).isEqualTo(2);
        assertThat(addedSinceFlush().get()).isEqualTo(1);
        assertThat(flushTriggered().get()).isFalse();
        TimeUnit.MILLISECONDS.sleep(500); // give a (wrongly) triggered flush the chance to run
        assertThat(lastTargetQuery(t1)).isEqualTo(t1.getLastTargetQuery());
        assertThat(lastTargetQuery(t2)).isEqualTo(t2.getLastTargetQuery());
        assertThat(queueSize().get()).isEqualTo(2);
    }

    // registers the target (eager write) and polls once more - the second poll is buffered
    private Target registerAndPoll(final String controllerId) throws InterruptedException {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, LOCALHOST);
        // so that the flushed poll time is after the registration time
        TimeUnit.MILLISECONDS.sleep(5);
        controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, LOCALHOST);
        return target;
    }

    // locks the target row in a transaction of another thread, until closed
    private TargetLock lock(final Target target) throws InterruptedException {
        final String tenant = AccessContext.tenant();
        final CountDownLatch locked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread thread = new Thread(() -> {
            try {
                AccessContext.asSystemAsTenant(tenant, () -> new TransactionTemplate(txManager).executeWithoutResult(_ -> {
                    entityManager.createNativeQuery("SELECT id FROM sp_target WHERE id = ?1 FOR UPDATE")
                            .setParameter(1, target.getId())
                            .getResultList();
                    locked.countDown();
                    try {
                        release.await(10, TimeUnit.SECONDS);
                    } catch (final InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                }));
            } catch (final Throwable e) {
                failure.set(e);
                locked.countDown();
            }
        }, "target-lock");
        thread.start();
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(failure.get()).isNull();
        return () -> {
            release.countDown();
            thread.join(TimeUnit.SECONDS.toMillis(10));
            assertThat(failure.get()).isNull();
        };
    }

    // the scheduled executor has a single thread and runs tasks in order - a no-op done means the tasks before it are done
    private void awaitExecutorIdle() throws Exception {
        scheduledExecutorService.submit(() -> {}).get(10, TimeUnit.SECONDS);
    }

    // entity manager whose first native query created by the given thread blocks until released
    private static EntityManager blockingFirstNativeQuery(
            final EntityManager delegate, final Thread thread, final CountDownLatch blocked, final CountDownLatch release) {
        final AtomicBoolean first = new AtomicBoolean(true);
        return (EntityManager) Proxy.newProxyInstance(PollPersistenceQueueTest.class.getClassLoader(),
                new Class<?>[] { EntityManager.class }, (_, method, args) -> {
                    if (Thread.currentThread() == thread && method.getName().equals("createNativeQuery")
                            && first.compareAndSet(true, false)) {
                        blocked.countDown();
                        assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private AtomicInteger queueSize() {
        return (AtomicInteger) ReflectionTestUtils.getField(lastPollUpdater, "queueSize");
    }

    private AtomicInteger addedSinceFlush() {
        return (AtomicInteger) ReflectionTestUtils.getField(lastPollUpdater, "addedSinceFlush");
    }

    private AtomicBoolean flushTriggered() {
        return (AtomicBoolean) ReflectionTestUtils.getField(lastPollUpdater, "flushTriggered");
    }

    private ReentrantLock flushLock() {
        return (ReentrantLock) ReflectionTestUtils.getField(lastPollUpdater, "flushLock");
    }

    private void resetPollEvents() {
        waitForEvents();
        pollEvents.clear();
    }

    private List<String> polledControllerIds() {
        return pollEvents.stream().flatMap(event -> event.getControllerIds().stream()).toList();
    }

    private long lastTargetQuery(final Target target) {
        return targetRepository.findById(target.getId()).orElseThrow().getLastTargetQuery();
    }

    @FunctionalInterface
    private interface TargetLock extends AutoCloseable {

        @Override
        void close() throws InterruptedException;
    }

    // Flushes the buffered polls, waits for a running flush: the flush only tries the lock (doesn't wait), here it is taken first -
    // reentrant, so the flush gets it
    private void flush() {
        final ReentrantLock lock = (ReentrantLock) ReflectionTestUtils.getField(lastPollUpdater, "flushLock");
        lock.lock();
        try {
            ReflectionTestUtils.invokeMethod(lastPollUpdater, "flush");
        } finally {
            lock.unlock();
        }
    }
}
