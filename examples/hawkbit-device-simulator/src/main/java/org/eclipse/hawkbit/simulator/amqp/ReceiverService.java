/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for all receiver objects.
 *
 *
 *
 */
public abstract class ReceiverService extends MessageService {

    /**
     * Constructor.
     *
     * @param rabbitTemplate
     *            RabbitTemplate
     * @param amqpProperties
     *            AmqpProperties
     * @param messageConverter
     *            MessageConverter
     */
    @Autowired
    public ReceiverService(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties) {
        super(rabbitTemplate, amqpProperties);
    }

    /**
     * Method to validate if content type is set in the message properties.
     *
     * @param message
     *            the message to get validated
     */
    protected void checkContentTypeJson(final Message message) {
        if (message.getBody().length == 0) {
            return;
        }
        final MessageProperties messageProperties = message.getMessageProperties();
        final String headerContentType = (String) messageProperties.getHeaders().get("content-type");
        if (null != headerContentType) {
            messageProperties.setContentType(headerContentType);
        }
        final String contentType = messageProperties.getContentType();
        if (contentType != null && contentType.contains("json")) {
            return;
        }
        throw new AmqpRejectAndDontRequeueException("Content-Type is not JSON compatible");
    }

}
