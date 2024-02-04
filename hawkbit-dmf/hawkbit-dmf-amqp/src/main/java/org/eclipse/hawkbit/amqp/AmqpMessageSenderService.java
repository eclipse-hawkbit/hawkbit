/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import java.net.URI;

import jakarta.validation.constraints.NotNull;

import org.springframework.amqp.core.Message;

/**
 * Interface to send a amqp message.
 */
@FunctionalInterface
public interface AmqpMessageSenderService {

    /**
     * Send the given message to the given uri. The uri contains the (virtual)
     * host and exchange e.g amqp://host/exchange.
     * 
     * @param message
     *            the amqp message
     * @param replyTo
     *            the reply to uri
     */
    void sendMessage(@NotNull final Message message, @NotNull final URI replyTo);
}