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

import org.eclipse.hawkbit.util.IpUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * A default implementation for the sender service. The service sends all amqp
 * message to the configured spring rabbitmq connections. The exchange is
 * extracted from the uri.
 */
public class DefaultAmqpSenderService implements AmqpSenderService {

    private final RabbitTemplate internalAmqpTemplate;

    /**
     * Constructor.
     * 
     * @param internalAmqpTemplate
     *            the amqp template
     */
    public DefaultAmqpSenderService(final RabbitTemplate internalAmqpTemplate) {
        this.internalAmqpTemplate = internalAmqpTemplate;
    }

    @Override
    public void sendMessage(final Message message, final URI replyTo) {
        if (!IpUtil.isAmqpUri(replyTo)) {
            return;
        }

        internalAmqpTemplate.send(extractExchange(replyTo), null, message);
    }

}
