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
import java.util.UUID;

import org.eclipse.hawkbit.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;

/**
 * A default implementation for the sender service. The service sends all amqp
 * message to the configured spring rabbitmq connections. The exchange is
 * extracted from the uri.
 */
public class DefaultAmqpSenderService implements AmqpSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAmqpSenderService.class);

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

        final String correlationId = UUID.randomUUID().toString();
        final String exchange = extractExchange(replyTo);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending message {} to exchange {} with correlationId {}", message, exchange, correlationId);
        } else {
            LOGGER.debug("Sending message to exchange {} with correlationId {}", exchange, correlationId);
        }

        internalAmqpTemplate.send(exchange, null, message, new CorrelationData(correlationId));
    }

}
