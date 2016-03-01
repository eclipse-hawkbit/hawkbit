/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * A base class which provide basis amqp staff.
 */
public class BaseAmqpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAmqpService.class);
    protected MessageConverter messageConverter;

    /**
     * Constructor.
     * 
     * @param messageConverter
     *            the message messageConverter.
     */
    public BaseAmqpService(final MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
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
    protected <T> T convertMessage(final Message message, final Class<T> clazz) {
        if (message == null) {
            return null;
        }
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getName());
        return (T) messageConverter.fromMessage(message);
    }

    /**
     * Is needed to convert a incoming message to is originally list object
     * type.
     *
     * @param message
     *            the message to convert.
     * @param clazz
     *            the class of the list content.
     * @return the list of converted objects
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> convertMessageList(final Message message, final Class<T> clazz) {
        if (message == null) {
            return Collections.emptyList();
        }
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                ArrayList.class.getName());
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME,
                clazz.getName());
        return (List<T>) messageConverter.fromMessage(message);
    }

    public MessageConverter getMessageConverter() {
        return messageConverter;
    }

    protected final String getStringHeaderKey(final Message message, final String key,
            final String errorMessageIfNull) {
        final Map<String, Object> header = message.getMessageProperties().getHeaders();
        final Object value = header.get(key);
        if (value == null) {
            logAndThrowMessageError(message, errorMessageIfNull);
        }
        return value.toString();
    }

    protected final void logAndThrowMessageError(final Message message, final String error) {
        LOGGER.error("Error \"{}\" reported by message {}", error, message.getMessageProperties().getMessageId());
        throw new IllegalArgumentException(error);
    }

}
