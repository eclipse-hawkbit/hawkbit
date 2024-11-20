/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import java.util.Iterator;
import java.util.List;

import org.springframework.util.ErrorHandler;

/**
 * An error handler chain that delegates the error to the matching error handler based on the type of exception
 */
public final class AmqpErrorHandlerChain {

    private final Iterator<AmqpErrorHandler> iterator;
    private final ErrorHandler defaultHandler;

    /**
     * Constructor.
     *
     * @param iterator the {@link AmqpErrorHandler} iterator
     * @param defaultHandler the default handler
     */
    private AmqpErrorHandlerChain(Iterator<AmqpErrorHandler> iterator, ErrorHandler defaultHandler) {
        this.iterator = iterator;
        this.defaultHandler = defaultHandler;
    }

    /**
     * Returns an {@link AmqpErrorHandlerChain}
     *
     * @param errorHandlers {@link List} of error handlers
     * @param defaultHandler the default error handler
     * @return an {@link AmqpErrorHandlerChain}
     */
    public static AmqpErrorHandlerChain getHandlerChain(final List<AmqpErrorHandler> errorHandlers, final ErrorHandler defaultHandler) {
        return new AmqpErrorHandlerChain(errorHandlers.iterator(), defaultHandler);
    }

    /**
     * Handles the error based on the type of exception
     *
     * @param error the throwable containing the cause of exception
     */
    public void handle(final Throwable error) {
        if (iterator.hasNext()) {
            final AmqpErrorHandler handler = iterator.next();
            handler.doHandle(error, this);
        } else {
            defaultHandler.handleError(error);
        }
    }
}