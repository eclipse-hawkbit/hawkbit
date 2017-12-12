/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.net.URI;

import javax.validation.constraints.NotNull;

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
