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

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * An abstract error handler for errors resulting from AMQP.
 */
public abstract class AbstractAmqpErrorHandler<T> implements AmqpErrorHandler {

    @Override
    public void doHandle(Throwable throwable, AmqpErrorHandlerChain chain) {
        // retrieving the cause of throwable as it contains the actual class of
        // exception
        final Throwable cause = throwable.getCause();
        if (getExceptionClass().isAssignableFrom(cause.getClass())) {
            throw new AmqpRejectAndDontRequeueException(getErrorMessage(throwable));
        } else {
            chain.handle(throwable);
        }
    }

    /**
     * Returns the class of the exception.
     *
     * @return the exception class
     */
    public abstract Class<T> getExceptionClass();

    /**
     * Returns the customized error message.
     *
     * @return the customized error message
     */
    public String getErrorMessage(Throwable throwable) {
        return AmqpErrorMessageComposer.constructErrorMessage(throwable);
    }

}
