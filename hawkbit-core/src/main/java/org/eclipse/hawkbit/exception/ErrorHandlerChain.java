/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Implementation of {@link EventHandlerChain} based on any kind of event to
 * iterate over all {@link ConditionalErrorHandler}
 */
public class ErrorHandlerChain<T> implements EventHandlerChain<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlerChain.class);

    private final Iterator<ConditionalErrorHandler<T>> iterator;
    private final Optional<Callable<?>> fallback;

    /**
     * Constructor
     *
     * @param iterator
     *            of {@link ConditionalErrorHandler}
     */
    ErrorHandlerChain(final Iterator<ConditionalErrorHandler<T>> iterator) {
        this(iterator, null);
    }

    /**
     * Constructor
     *
     * @param iterator
     *            of {@link ConditionalErrorHandler}
     */
    ErrorHandlerChain(final Iterator<ConditionalErrorHandler<T>> iterator, final Callable<Void> fallback) {
        this.iterator = iterator;
        this.fallback = Optional.ofNullable(fallback);
    }

    /**
     * Create an instance of {@link ErrorHandlerChain} by a given list of
     * {@link ConditionalErrorHandler}
     *
     * @param errorHandlers
     *            list of {@link ConditionalErrorHandler}
     * @return instance of {@link ErrorHandlerChain}
     * @param <T>
     *            the event type
     */
    public static <T> ErrorHandlerChain<T> getHandler(final List<ConditionalErrorHandler<T>> errorHandlers) {
        return new ErrorHandlerChain<>(errorHandlers.iterator());
    }

    /**
     * Create an instance of {@link ErrorHandlerChain} by a given list of
     * {@link ConditionalErrorHandler}
     *
     * @param errorHandlers
     *            list of {@link ConditionalErrorHandler}
     * @param callable
     *            this callable will be called in case no handler can process the
     *            event
     * @return instance of {@link ErrorHandlerChain}
     * @param <T>
     *            the event type
     */
    public static <T> ErrorHandlerChain<T> getHandler(final List<ConditionalErrorHandler<T>> errorHandlers,
            Callable<Void> callable) {
        return new ErrorHandlerChain<>(errorHandlers.iterator(), callable);
    }

    @Override
    public void doHandle(T event) {
        if (iterator.hasNext()) {
            final ConditionalErrorHandler<T> handler = iterator.next();
            handler.handle(event, this);
        } else {
            fallback.ifPresent(callable -> {
                try {
                    callable.call();
                } catch (Exception e) {
                    LOGGER.error("Could not handle event with fallback mechanism.", e);
                }
            });
        }
    }
}
