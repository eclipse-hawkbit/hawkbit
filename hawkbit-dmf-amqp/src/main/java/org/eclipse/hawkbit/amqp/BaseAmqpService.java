/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.amqp;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 *
 */
public class BaseAmqpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAmqpService.class);
    protected static final String VIRTUAL_HOST_MESSAGE_HEADER = "VHOST_HEADER";

    protected MessageConverter messageConverter;

    protected RabbitTemplate spInternalConnectorTemplate;

    public BaseAmqpService(final MessageConverter messageConverter, final RabbitTemplate defaultTemplate) {
        this.messageConverter = messageConverter;
        spInternalConnectorTemplate = defaultTemplate;
    }

    protected String getVirtualHost(final Message message) {
        final Object virtualHost = message.getMessageProperties().getHeaders().get(VIRTUAL_HOST_MESSAGE_HEADER);

        if (virtualHost == null) {
            return spInternalConnectorTemplate.getConnectionFactory().getVirtualHost();
        }
        return virtualHost.toString();
    }

    protected void cleanMessage(final Message message) {
        message.getMessageProperties().getHeaders().remove(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);
        message.getMessageProperties().getHeaders().remove(VIRTUAL_HOST_MESSAGE_HEADER);
    }

    /**
     * Is needed to convert a incoming message to is originally object type.
     *
     * @param message
     *            the message to convert.
     * @param clazz
     *            the class of the originally object.
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> T convertMessage(final Message message, final Class<T> clazz) {
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getTypeName());
        return (T) messageConverter.fromMessage(message);
    }

    protected String getExchangeFromAmqpUri(final URI amqpUri) {
        return amqpUri.getPath().substring(1);
    }

    protected String getStringHeaderKey(final Message message, final String key, final String errorMessageIfNull) {
        final Map<String, Object> header = message.getMessageProperties().getHeaders();
        final Object value = header.get(key);
        if (value == null) {
            logAndThrowMessageError(message, errorMessageIfNull);
        }
        return value.toString();
    }

    protected void logAndThrowMessageError(final Message message, final String error) {
        LOGGER.error("Error \"{}\" reported by message {}", error, message.getMessageProperties().getMessageId());
        throw new IllegalArgumentException(error);
    }

}
