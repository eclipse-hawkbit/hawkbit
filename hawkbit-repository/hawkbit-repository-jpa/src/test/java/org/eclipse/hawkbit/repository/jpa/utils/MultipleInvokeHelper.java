/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.concurrent.Callable;

/**
 * Helper to call a request multiple times regarding a given condition until
 * timeout is reached.
 *
 */
public final class MultipleInvokeHelper {

    /**
     * Call with timeout until result is not null.
     * 
     * @param callable
     *            class
     * @param timeout
     *            value
     * @param pollInterval
     *            value
     * @return
     * @throws Exception
     */
    public static <T> T doWithTimeoutUntilResultIsNotNull(final Callable<T> callable, final long timeout,
            final long pollInterval) throws Exception // NOPMD
    {
        return doWithTimeout(callable, new SuccessCondition<T>() {
            @Override
            public boolean success(final T result) {
                return result != null;
            };
        }, timeout, pollInterval);
    }

    /**
     * Call with timeout.
     * 
     * @param callable
     *            class
     * @param successCondition
     *            class
     * @param timeout
     *            value
     * @param pollInterval
     *            value
     * @return
     * @throws Exception
     */
    public static <T> T doWithTimeout(final Callable<T> callable, final SuccessCondition<T> successCondition,
            final long timeout, final long pollInterval) throws Exception // NOPMD
    {

        if (pollInterval < 0) {
            throw new IllegalArgumentException("pollInterval must non negative");
        }

        long duration = 0;
        Exception exception = null;
        T returnValue = null;
        while (untilTimeoutReached(timeout, duration)) {
            try {
                returnValue = callable.call();
                // clear exception
                exception = null;
            } catch (final Exception ex) {
                exception = ex;
            }
            Thread.sleep(pollInterval);
            duration += pollInterval > 0 ? pollInterval : 1;
            if (exception == null && successCondition.success(returnValue)) {
                return returnValue;
            } else {
                returnValue = null;
            }
        }
        if (exception != null) {
            throw exception;
        }
        return returnValue;
    }

    private static boolean untilTimeoutReached(final long timeout, final long duration) {
        return duration <= timeout || timeout < 0;
    }

}
