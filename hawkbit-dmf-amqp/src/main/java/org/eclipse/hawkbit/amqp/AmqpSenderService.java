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
     * @param uri
     *            the reply to uri
     */
    void sendMessage(Message message, URI uri);

    /**
     * Extract the exchange from the uri. Default implementation removes the
     * first /.
     * 
     * @param amqpUri
     *            the amqp uri
     * @return the exchange.
     */
    default String extractExchange(final URI amqpUri) {
        return amqpUri.getPath().substring(1);
    }

}
