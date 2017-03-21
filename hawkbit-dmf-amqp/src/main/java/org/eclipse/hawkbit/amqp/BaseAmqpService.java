/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * A base class which provide basis amqp staff.
 */
public class BaseAmqpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAmqpService.class);
    private final RabbitTemplate rabbitTemplate;

    /**
     * Constructor.
     * 
     * @param rabbitTemplate
     *            the rabbit template.
     */
    public BaseAmqpService(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    protected static void checkContentTypeJson(final Message message) {
        final MessageProperties messageProperties = message.getMessageProperties();
        if (messageProperties.getContentType() != null && messageProperties.getContentType().contains("json")) {
            return;
        }
        throw new AmqpRejectAndDontRequeueException("Content-Type is not JSON compatible");
    }

    /**
     * Is needed to convert a incoming message to is originally object type.
     *
     * @param message
     *            the message to convert.
     * @param clazz
     *            the class of the originally object.
     * @return the converted object
     */
    @SuppressWarnings("unchecked")
    public <T> T convertMessage(@NotNull final Message message, final Class<T> clazz) {
        checkMessageBody(message);
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }

    protected MessageConverter getMessageConverter() {
        return rabbitTemplate.getMessageConverter();
    }

    private static boolean isMessageBodyEmpty(final Message message) {
        return message.getBody() == null || message.getBody().length == 0;
    }

    protected void checkMessageBody(@NotNull final Message message) {
        if (isMessageBodyEmpty(message)) {
            throw new MessageConversionException("Message body cannot be null");
        }
    }

    protected String getStringHeaderKey(final Message message, final String key, final String errorMessageIfNull) {
        final Map<String, Object> header = message.getMessageProperties().getHeaders();
        final Object value = header.get(key);
        if (value == null) {
            logAndThrowMessageError(message, errorMessageIfNull);
            return null;
        }
        return value.toString();
    }

    protected static final void logAndThrowMessageError(final Message message, final String error) {
        LOGGER.warn("Warning! \"{}\" reported by message: {}", error, message);
        throw new AmqpRejectAndDontRequeueException(error);
    }

    protected RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }

    /**
     * Clean message properties before sending a message.
     * 
     * @param message
     *            the message to cleaned up
     */
    protected void cleanMessageHeaderProperties(final Message message) {
        message.getMessageProperties().getHeaders().remove(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);
    }

}
