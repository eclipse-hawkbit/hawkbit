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
import java.nio.charset.StandardCharsets;
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
public class DefaultAmqpMessageSenderService extends BaseAmqpService implements AmqpMessageSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAmqpMessageSenderService.class);

    /**
     * Constructor.
     * 
     * @param rabbitTemplate
     *            the AMQP template
     */
    public DefaultAmqpMessageSenderService(final RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    public void sendMessage(final Message message, final URI sendTo) {
        if (!IpUtil.isAmqpUri(sendTo)) {
            return;
        }

        final String exchange = sendTo.getPath().substring(1);
        final String correlationId = UUID.randomUUID().toString();

        if (isCorrelationIdEmpty(message)) {
            message.getMessageProperties().setCorrelationId(correlationId.getBytes(StandardCharsets.UTF_8));
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending message {} to exchange {} with correlationId {}", message, exchange, correlationId);
        } else {
            LOGGER.debug("Sending message to exchange {} with correlationId {}", exchange, correlationId);
        }

        getRabbitTemplate().send(exchange, null, message, new CorrelationData(correlationId));
    }

    protected static boolean isCorrelationIdEmpty(final Message message) {
        return message.getMessageProperties().getCorrelationId() == null
                || message.getMessageProperties().getCorrelationId().length <= 0;
    }

}
