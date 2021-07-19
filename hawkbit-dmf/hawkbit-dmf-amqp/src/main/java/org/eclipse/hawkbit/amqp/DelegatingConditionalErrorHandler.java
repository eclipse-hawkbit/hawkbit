/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.util.List;
import javax.validation.constraints.NotNull;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.util.ErrorHandler;

/**
 * An error handler delegates error handling to the matching {@link AmqpErrorHandler} based on the type of exception
 */
public class DelegatingConditionalErrorHandler implements ErrorHandler {
    private final List<AmqpErrorHandler> handlers;
    private final ErrorHandler defaultHandler;

    /**
     * Constructor
     *
     * @param handlers
     *                 {@link List} of error handlers
     * @param defaultHandler
     *                  the default error handler
     */
    public DelegatingConditionalErrorHandler(final List<AmqpErrorHandler> handlers, @NotNull final ErrorHandler defaultHandler) {
        this.handlers = handlers;
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void handleError(final Throwable t) {
        if (t.getCause() == null || includesAmqpRejectException(t)){
            return;
        }
        AmqpErrorHandlerChain.getHandlerChain(handlers, defaultHandler).handle(t);
    }

    private boolean includesAmqpRejectException(final Throwable t) {
        if (t instanceof AmqpRejectAndDontRequeueException){
            return true;
        }
        if (t.getCause() != null) {
            return includesAmqpRejectException(t.getCause());
        }
        return false;
    }
}
