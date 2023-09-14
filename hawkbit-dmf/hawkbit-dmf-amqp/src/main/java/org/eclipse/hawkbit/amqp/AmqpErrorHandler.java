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

/**
 * Interface declaration of {@link AmqpErrorHandler} that handles errors based on the
 * types of exception.
 */
@FunctionalInterface
public interface AmqpErrorHandler {

    /**
     * Handles the error based on the type of exception
     *
     * @param throwable
     *            the throwable
     * @param chain
     *            an {@link AmqpErrorHandlerChain}
     */
void doHandle(final Throwable throwable, final AmqpErrorHandlerChain chain);

}
