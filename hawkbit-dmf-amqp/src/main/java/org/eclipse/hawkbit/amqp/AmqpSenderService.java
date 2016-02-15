package org.eclipse.hawkbit.amqp;

import java.net.URI;

import org.springframework.amqp.core.Message;

/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */

/**
 *
 */
@FunctionalInterface
public interface AmqpSenderService {

    /**
     * 
     * @param message
     * @param uri
     */
    void sendMessage(Message message, URI uri);

}
