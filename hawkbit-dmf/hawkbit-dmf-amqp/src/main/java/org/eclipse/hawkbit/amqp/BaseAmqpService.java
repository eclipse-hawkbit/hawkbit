/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.ObjectUtils;

/**
 * A base class which provide basis amqp staff.
 */
@Slf4j
public class BaseAmqpService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Constructor.
     *
     * @param rabbitTemplate the rabbit template.
     */
    public BaseAmqpService(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Is needed to convert a incoming message to is originally object type.
     *
     * @param message the message to convert.
     * @param clazz the class of the originally object.
     * @return the converted object
     */
    @SuppressWarnings("unchecked")
    public <T> T convertMessage(@NotNull final Message message, final Class<T> clazz) {
        checkMessageBody(message);
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME, clazz.getName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }

    protected static void checkContentTypeJson(final Message message) {
        final MessageProperties messageProperties = message.getMessageProperties();
        if (messageProperties.getContentType() != null && messageProperties.getContentType().contains("json")) {
            return;
        }
        throw new AmqpRejectAndDontRequeueException("Content-Type is not JSON compatible");
    }

    protected static boolean isMessageBodyEmpty(final Message message) {
        return ObjectUtils.isEmpty(message.getBody());
    }

    protected static void logAndThrowMessageError(final Message message, final String error) {
        log.debug("Warning! \"{}\" reported by message: {}", error, message);
        throw new AmqpRejectAndDontRequeueException(error);
    }

    protected MessageConverter getMessageConverter() {
        return rabbitTemplate.getMessageConverter();
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

    protected RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }

    /**
     * Clean message properties before sending a message.
     *
     * @param message the message to cleaned up
     */
    protected void cleanMessageHeaderProperties(final Message message) {
        message.getMessageProperties().getHeaders().remove(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);
    }
}