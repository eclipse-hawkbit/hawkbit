/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf.amqp;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.dmf.DmfProperties;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;

/**
 * Abstract class for sender and receiver service.
 */
@Slf4j
class MessageService {

    protected final RabbitTemplate rabbitTemplate;
    protected final DmfProperties dmfProperties;

    MessageService(final RabbitTemplate rabbitTemplate, final DmfProperties dmfProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.dmfProperties = dmfProperties;
    }

    /**
     * Convert a message body to a given class and set the message header
     * AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME for Jackson converter.
     */
    @SuppressWarnings("unchecked")
    <T> T convertMessage(final Message message, final Class<T> clazz) {
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getTypeName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }
}