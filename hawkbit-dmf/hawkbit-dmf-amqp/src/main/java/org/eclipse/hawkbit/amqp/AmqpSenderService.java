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

import org.eclipse.hawkbit.util.IpUtil;
import org.springframework.amqp.core.Message;

/**
 * Interface to send a amqp message.
 */
@FunctionalInterface
public interface AmqpSenderService {

    /**
     * Send the given message to the given uri. The uri contains the (virtual)
     * host and exchange e.g amqp://host/exchange.
     * 
     * @param message
     *            the amqp message
     * @param replyTo
     *            the reply to uri
     */
    default void sendMessage(@NotNull final Message message, @NotNull final URI replyTo) {
        if (!IpUtil.isAmqpUri(replyTo)) {
            return;
        }

        sendMessage(message, replyTo.getPath().substring(1), replyTo.getHost());
    }

    /**
     * Send the given message to the given host and exchange.
     * 
     * @param message
     *            the amqp message
     * @param exchange
     *            to send to
     * @param virtualHost
     *            to send to
     */
    void sendMessage(@NotNull final Message message, @NotNull final String exchange, @NotNull final String virtualHost);

}
