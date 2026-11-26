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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Persistence of the buffered (lazy) target polls. The flush is triggered directly, the scheduled flush is effectively
 * off (flush time of one hour).
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Poll persistence
 */
@TestPropertySource(locations = "classpath:/jpa-test.properties", properties = {
        "hawkbit.server.repository.eagerPollPersistence=false",
        "hawkbit.server.repository.pollPersistenceFlushTime=3600000" })
class PollPersistenceTest extends AbstractJpaIntegrationTest {

    private static final String OTHER_TENANT = "POLL_PERSISTENCE_OTHER";

    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationEventMulticaster eventMulticaster;
    @Autowired
    private RepositoryProperties repositoryProperties;

    private final List<TargetPollEvent> pollEvents = new CopyOnWriteArrayList<>();
    private final ApplicationListener<TargetPollEvent> pollEventListener = pollEvents::add;

    private JpaControllerManagement jpaControllerManagement;

    @AfterEach
    void removePollEventListener() {
        eventMulticaster.removeApplicationListener(pollEventListener);
    }

    @BeforeEach
    void setUp() {
        eventMulticaster.addApplicationListener(pollEventListener);
        jpaControllerManagement = AopTestUtils.getUltimateTargetObject(controllerManagement);
        // nothing left from other tests
        jpaControllerManagement.flushUpdateQueue();
    }

    /**
     * Verifies that a failure while persisting the polls of one tenant doesn't drop the polls of another tenant, and that
     * the failed polls stay buffered and are written by the next flush.
     */
    @Test
    void failureForOneTenantKeepsOtherTenantsAndRebuffers() throws Exception {
        final Target other = asController(OTHER_TENANT, () -> registerAndPoll("failure-other"));
        final Target own = registerAndPoll("failure-own");

        final EntityManager original = (EntityManager) ReflectionTestUtils.getField(jpaControllerManagement, "entityManager");
        ReflectionTestUtils.setField(jpaControllerManagement, "entityManager", failingForTenant(original, OTHER_TENANT));
        try {
            jpaControllerManagement.flushUpdateQueue();
        } finally {
            ReflectionTestUtils.setField(jpaControllerManagement, "entityManager", original);
        }

        assertThat(lastTargetQuery(own)).isGreaterThan(own.getLastTargetQuery());
        assertThat(asTenant(OTHER_TENANT, () -> lastTargetQuery(other))).isEqualTo(other.getLastTargetQuery());

        jpaControllerManagement.flushUpdateQueue();

        assertThat(asTenant(OTHER_TENANT, () -> lastTargetQuery(other))).isGreaterThan(other.getLastTargetQuery());
    }

    /**
     * Verifies that the flush doesn't wait for a target locked by another transaction: the unlocked target is written, the
     * locked one keeps its last poll time, and a poll event is published only for the written target.
     */
    @Test
    void lockedTargetIsSkippedWithoutWaiting() throws Exception {
        final Target t1 = registerAndPoll("locked-skipped-t1");
        final Target t2 = registerAndPoll("locked-skipped-t2");
        resetPollEvents();

        try (TargetLock ignored = lock(t2)) {
            final long start = System.nanoTime();
            jpaControllerManagement.flushUpdateQueue();
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
        }
        waitForEvents();

        assertThat(lastTargetQuery(t1)).isGreaterThan(t1.getLastTargetQuery());
        assertThat(lastTargetQuery(t2)).isEqualTo(t2.getLastTargetQuery());
        assertThat(polledControllerIds()).containsExactly(t1.getControllerId());
    }

    /**
     * Verifies that the poll of a target skipped because it was locked stays buffered and is written by the next flush
     * after the lock is released - with its own poll time, not the time of the later flush. A target polled again in the
     * meantime is written in the same statement with the later flush time, and each poll time gets its own poll event.
     */
    @Test
    void lockedTargetPersistedInLaterRun() throws Exception {
        final Target t1 = registerAndPoll("locked-later-t1");
        final Target t2 = registerAndPoll("locked-later-t2");
        try (TargetLock ignored = lock(t2)) {
            jpaControllerManagement.flushUpdateQueue();
        }
        assertThat(lastTargetQuery(t2)).isEqualTo(t2.getLastTargetQuery());
        final long t1FirstFlush = lastTargetQuery(t1);
        assertThat(t1FirstFlush).isGreaterThan(t1.getLastTargetQuery());
        resetPollEvents();
        TimeUnit.MILLISECONDS.sleep(10);
        // t1 polls again - the later flush has a fresh poll (t1) and a queued again one (t2) with different poll times
        controllerManagement.findOrRegisterTargetIfItDoesNotExist(t1.getControllerId(), LOCALHOST);
        final long beforeLaterFlush = System.currentTimeMillis();

        jpaControllerManagement.flushUpdateQueue();
        waitForEvents();

        final long t2Written = lastTargetQuery(t2);
        final long t1Written = lastTargetQuery(t1);
        assertThat(t2Written).isGreaterThan(t2.getLastTargetQuery()).isLessThan(beforeLaterFlush);
        assertThat(t1Written).isGreaterThanOrEqualTo(beforeLaterFlush);
        assertThat(pollEvents).hasSize(2).anySatisfy(event -> {
            assertThat(event.getControllerIds()).containsExactly(t2.getControllerId());
            assertThat(event.getLastTargetPoll()).isEqualTo(t2Written);
        }).anySatisfy(event -> {
            assertThat(event.getControllerIds()).containsExactly(t1.getControllerId());
            assertThat(event.getLastTargetPoll()).isEqualTo(t1Written);
        });
    }

    /**
     * Verifies that a poll that couldn't be persisted within the max queue time is dropped and not retried by the next flush.
     */
    @Test
    void lockedTargetPollDroppedAfterMaxQueueTime() throws Exception {
        final Target t1 = registerAndPoll("expired-t1");
        final long maxQueueTime = repositoryProperties.getPollPersistenceMaxQueueTime();
        repositoryProperties.setPollPersistenceMaxQueueTime(0);
        try (final TargetLock _ = lock(t1)) {
            TimeUnit.MILLISECONDS.sleep(5); // the poll is older than the max queue time (0)
            jpaControllerManagement.flushUpdateQueue();
        } finally {
            repositoryProperties.setPollPersistenceMaxQueueTime(maxQueueTime);
        }

        jpaControllerManagement.flushUpdateQueue();

        assertThat(lastTargetQuery(t1)).isEqualTo(t1.getLastTargetQuery());
    }

    /**
     * Verifies that a buffered poll doesn't replace a newer last poll time written directly in the meantime.
     */
    @Test
    void lastPollTimeNeverMovesBackwards() throws Exception {
        final Target t1 = registerAndPoll("backwards-t1");
        // a direct write after the poll was buffered, newer than any flush time of this test
        final long newer = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        new TransactionTemplate(txManager).executeWithoutResult(status ->
                entityManager.createNativeQuery("UPDATE sp_target SET last_target_query = ?1 WHERE id = ?2")
                        .setParameter(1, newer)
                        .setParameter(2, t1.getId())
                        .executeUpdate());

        jpaControllerManagement.flushUpdateQueue();

        assertThat(lastTargetQuery(t1)).isEqualTo(newer);
    }

    /**
     * Verifies that the buffered poll time is written although the target was changed by another transaction after the
     * poll was buffered, and that the flush doesn't change the target's optimistic lock version.
     */
    @Test
    void pollTimeWrittenAfterConcurrentTargetChangeWithoutVersionChange() throws Exception {
        final Target t1 = registerAndPoll("concurrent-change-t1");
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            final JpaTarget target = targetRepository.findById(t1.getId()).orElseThrow();
            target.setAddress("http://changed");
            targetRepository.save(target);
        });
        final JpaTarget changed = targetRepository.findById(t1.getId()).orElseThrow();
        assertThat(changed.getOptLockRevision()).isNotEqualTo(t1.getOptLockRevision());

        jpaControllerManagement.flushUpdateQueue();

        final JpaTarget after = targetRepository.findById(t1.getId()).orElseThrow();
        assertThat(after.getLastTargetQuery()).isGreaterThan(changed.getLastTargetQuery());
        assertThat(after.getOptLockRevision()).isEqualTo(changed.getOptLockRevision());
        assertThat(after.getAddress()).isEqualTo("http://changed");
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
                AccessContext.asSystemAsTenant(tenant, () -> new TransactionTemplate(txManager).executeWithoutResult(status -> {
                    entityManager.createNativeQuery("SELECT id FROM sp_target WHERE id = ?1 FOR UPDATE")
                            .setParameter(1, target.getId())
                            .getResultList();
                    locked.countDown();
                    try {
                        release.await(10, TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
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

    private static <T> T asController(final String tenant, final Callable<T> callable) throws Exception {
        return SecurityContextSwitch.callAs(
                SecurityContextSwitch.withTenantAndUser(tenant, "controller", new String[] { SpRole.CONTROLLER_ROLE }, true, true), callable);
    }

    private static <T> T asTenant(final String tenant, final Callable<T> callable) throws Exception {
        return SecurityContextSwitch.callAs(SecurityContextSwitch.withTenantAndUserAndAllPermissions(tenant, "user"), callable);
    }

    // entity manager whose native queries fail when executed with the given tenant as parameter
    private static EntityManager failingForTenant(final EntityManager delegate, final String tenant) {
        return (EntityManager) Proxy.newProxyInstance(PollPersistenceTest.class.getClassLoader(), new Class<?>[] { EntityManager.class },
                (proxy, method, args) -> {
                    final Object result = invoke(delegate, method, args);
                    return result instanceof Query query && method.getName().equals("createNativeQuery")
                            ? failingForTenant(query, tenant, new boolean[1])
                            : result;
                });
    }

    private static Query failingForTenant(final Query delegate, final String tenant, final boolean[] tenantSet) {
        return (Query) Proxy.newProxyInstance(PollPersistenceTest.class.getClassLoader(), new Class<?>[] { Query.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("setParameter") && args.length > 1 && Objects.equals(args[1], tenant)) {
                        tenantSet[0] = true;
                    }
                    if (tenantSet[0] && (method.getName().equals("executeUpdate") || method.getName().startsWith("getResult"))) {
                        throw new PersistenceException("Injected failure for tenant " + tenant);
                    }
                    final Object result = invoke(delegate, method, args);
                    return result == delegate ? proxy : result;
                });
    }

    private static Object invoke(final Object target, final Method method, final Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
