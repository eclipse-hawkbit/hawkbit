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

import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.util.ErrorHandler;

/**
 * An error handler delegates error handling to the matching {@link AmqpErrorHandler} based on the type of exception
 */
@Slf4j
public class DelegatingConditionalErrorHandler implements ErrorHandler {

    private final List<AmqpErrorHandler> handlers;
    private final ErrorHandler defaultHandler;

    /**
     * Constructor
     *
     * @param handlers {@link List} of error handlers
     * @param defaultHandler the default error handler
     */
    public DelegatingConditionalErrorHandler(final List<AmqpErrorHandler> handlers, @NotNull final ErrorHandler defaultHandler) {
        this.handlers = handlers;
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void handleError(final Throwable t) {
        if (t.getCause() == null) {
            log.error("Cannot handle the error as the cause of the error is null!");
            return;
        }

        if (includesAmqpRejectException(t.getCause())) {
            log.error("Received an AmqpRejectAndDontRequeueException due to {}", t.getCause().getMessage());
            return;
        }

        AmqpErrorHandlerChain.getHandlerChain(handlers, defaultHandler).handle(t);
    }

    private boolean includesAmqpRejectException(final Throwable t) {
        if (t instanceof AmqpRejectAndDontRequeueException) {
            return true;
        }
        if (t.getCause() != null) {
            return includesAmqpRejectException(t.getCause());
        }
        return false;
    }
}