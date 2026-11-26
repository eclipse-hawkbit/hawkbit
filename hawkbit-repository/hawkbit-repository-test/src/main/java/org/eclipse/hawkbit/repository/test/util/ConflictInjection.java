/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.Advised;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Deterministic injection of a concurrent ("device") write between a read and the following writes of the code under
 * test:
 *
 * <pre>{@code
 * try (ConflictInjection ignored = ConflictInjection.afterRead(txManager, actionRepository,
 *         "findWithTargetByIdIn", () -> finishAction(manualAction))) {
 *     rolloutHandler.handleAll();
 * }
 * }</pre>
 *
 * The intercepted method first runs for real, then - on its first call only - the callback runs and commits in a
 * separate {@code REQUIRES_NEW} transaction (own connection), as a concurrent request would. The result read before
 * the callback is returned, so the caller continues with the now stale state.
 * <p/>
 * It adds an interceptor to the Spring AOP proxy of the bean (e.g. a Spring Data repository, also when wrapped by the
 * hawkBit access control / cache proxy, which passes {@link Advised} calls through), instead of using a Mockito spy
 * bean: the hawkBit repository post-processing ({@code withProxy}) is invoked on the spy, delegates to the real
 * repository and so replaces the spy with the real bean again. The interceptor is removed on {@link #close()}, as the
 * bean is shared by the cached test context.
 * <p/>
 * Note: the callback must not touch rows the caller has already locked in its (suspended) transaction - that would wait
 * for the lock on the same thread.
 */
public final class ConflictInjection implements AutoCloseable {

    private final Advised advised;
    private final MethodInterceptor interceptor;

    private ConflictInjection(final Advised advised, final MethodInterceptor interceptor) {
        this.advised = advised;
        this.interceptor = interceptor;
    }

    /**
     * Runs the callback once, in its own committed transaction, after the first call of the given method of the bean.
     *
     * @param txManager transaction manager to start the {@code REQUIRES_NEW} transaction with
     * @param bean Spring AOP proxied bean (e.g. a repository)
     * @param methodName name of the read method to intercept (all overloads)
     * @param callback concurrent write
     * @return handle that removes the interception on close
     */
    public static ConflictInjection afterRead(
            final PlatformTransactionManager txManager, final Object bean, final String methodName, final Runnable callback) {
        return inject(txManager, bean, methodName, callback, true);
    }

    /**
     * Like {@link #afterRead(PlatformTransactionManager, Object, String, Runnable)}, but runs the callback after every call
     * of the method, e.g. to make every retry of the caller conflict again.
     *
     * @param txManager transaction manager to start the {@code REQUIRES_NEW} transaction with
     * @param bean Spring AOP proxied bean (e.g. a repository)
     * @param methodName name of the read method to intercept (all overloads)
     * @param callback concurrent write
     * @return handle that removes the interception on close
     */
    public static ConflictInjection afterEachRead(
            final PlatformTransactionManager txManager, final Object bean, final String methodName, final Runnable callback) {
        return inject(txManager, bean, methodName, callback, false);
    }

    private static ConflictInjection inject(
            final PlatformTransactionManager txManager, final Object bean, final String methodName, final Runnable callback,
            final boolean once) {
        if (!(bean instanceof Advised advised)) {
            throw new IllegalArgumentException("Not a Spring AOP proxy: " + bean.getClass());
        }
        final TransactionTemplate requiresNew = new TransactionTemplate(txManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        final AtomicBoolean done = new AtomicBoolean();
        final AtomicBoolean running = new AtomicBoolean();
        final MethodInterceptor interceptor = invocation -> {
            final Object result = invocation.proceed();
            // set before running, so calls of the same method within the callback don't recurse
            if (invocation.getMethod().getName().equals(methodName) && (once
                    ? done.compareAndSet(false, true)
                    : running.compareAndSet(false, true))) {
                try {
                    requiresNew.executeWithoutResult(status -> callback.run());
                } finally {
                    if (!once) {
                        running.set(false);
                    }
                }
            }
            return result;
        };
        // first in the chain: runs outside the repository's own transaction interceptor, i.e. after its read completed
        advised.addAdvice(0, interceptor);
        return new ConflictInjection(advised, interceptor);
    }

    @Override
    public void close() {
        advised.removeAdvice(interceptor);
    }
}
