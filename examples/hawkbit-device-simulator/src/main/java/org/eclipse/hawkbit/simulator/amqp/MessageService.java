/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for sender and receiver service.
 *
 *
 *
 */
public class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    protected RabbitTemplate rabbitTemplate;

    protected AmqpProperties amqpProperties;

    /**
     * Constructor.
     *
     * @param rabbitTemplate
     *            the rabbit template
     * @param amqpProperties
     *            the amqp properties
     * @param messageConverter
     *            the message converter
     */
    @Autowired
    public MessageService(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpProperties = amqpProperties;
    }

    public void setAmqpProperties(final AmqpProperties amqpProperties) {
        this.amqpProperties = amqpProperties;
    }

    public void setRabbitTemplate(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Method to call when error emerges.
     * 
     * @param message
     *            the message that triggered the error
     * @param error
     *            the error
     */
    public void logAndThrowMessageError(final Message message, final String error) {
        LOGGER.error("Error \"{}\" reported by message {}", error, message.getMessageProperties().getMessageId());
        throw new IllegalArgumentException(error);
    }

    /**
     * Convert a message body to a given class and set the message header
     * AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME for Jackson converter.
     * 
     * @param message
     *            which body will converted
     * @param clazz
     *            the body class
     * @return the converted body
     */
    @SuppressWarnings("unchecked")
    public <T> T convertMessage(final Message message, final Class<T> clazz) {
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getTypeName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }
}
