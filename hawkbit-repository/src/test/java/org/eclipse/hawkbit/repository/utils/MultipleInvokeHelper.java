/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.utils;

import java.util.concurrent.Callable;

/**
 * Helper to call a request multiple times regarding a given condition until
 * timeout is reached.
 * 
 * @author Jonathan Knoblauch
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
