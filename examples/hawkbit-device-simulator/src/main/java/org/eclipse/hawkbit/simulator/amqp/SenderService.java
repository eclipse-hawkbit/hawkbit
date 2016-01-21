/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract calls for sender service objects.
 *
 *
 *
 */
public abstract class SenderService extends MessageService {

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
     * @param adress
     *            the exchange name
     * @param message
     *            the amqp message which will be send if its not null
     */
    public void sendMessage(final String adress, final Message message) {
        if (message == null) {
            return;
        }
        rabbitTemplate.setExchange(adress);
        rabbitTemplate.send(message);
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
