/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract calls for sender service objects.
 *
 *
 *
 */
public abstract class SenderService extends MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SenderService.class);

    /**
     * Constructor for sender service.
     *
     * @param rabbitTemplate
     *            the rabbit template
     * @param amqpProperties
     *            the amqp properties
     * @param cacheManager
     *            the cache manager
     */
    @Autowired
    public SenderService(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties) {
        super(rabbitTemplate, amqpProperties);
    }

    /**
     * Send a message if the message is not null.
     *
     * @param address
     *            the exchange name
     * @param message
     *            the amqp message which will be send if its not null
     */
    public void sendMessage(final String address, final Message message) {
        if (message == null) {
            return;
        }
        message.getMessageProperties().getHeaders().remove(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);

        final String correlationId = UUID.randomUUID().toString();

        if (isCorrelationIdEmpty(message)) {
            message.getMessageProperties().setCorrelationId(correlationId.getBytes(StandardCharsets.UTF_8));
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending message {} to exchange {} with correlationId {}", message, address, correlationId);
        } else {
            LOGGER.debug("Sending message to exchange {} with correlationId {}", address, correlationId);
        }

        rabbitTemplate.send(address, null, message, new CorrelationData(correlationId));
    }

    private static boolean isCorrelationIdEmpty(final Message message) {
        return message.getMessageProperties().getCorrelationId() == null
                || message.getMessageProperties().getCorrelationId().length <= 0;
    }

    /**
     * Convert object and message properties to message.
     *
     * @param object
     *            to get converted
     * @param messageProperties
     *            to get converted
     * @return converted message
     */
    public Message convertMessage(final Object object, final MessageProperties messageProperties) {
        return rabbitTemplate.getMessageConverter().toMessage(object, messageProperties);
    }

}
